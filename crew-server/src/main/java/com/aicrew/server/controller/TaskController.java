package com.aicrew.server.controller;

import com.aicrew.common.dto.Result;
import com.aicrew.common.entity.*;
import com.aicrew.common.enums.MsgType;
import com.aicrew.common.enums.SenderType;
import com.aicrew.common.enums.TaskStatus;
import com.aicrew.common.exception.BizException;
import com.aicrew.common.repository.*;
import com.aicrew.engine.ChatStreamService;
import com.aicrew.engine.Orchestrator;
import com.aicrew.server.dto.StepVo;
import com.aicrew.server.dto.TaskCreateRequest;
import com.aicrew.server.dto.TaskDetailVo;
import com.aicrew.server.dto.BossActionRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** 需求任务：发布 / 作战室详情 / SSE 实时流 / BOSS 验收动作 */
@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskRepository taskRepository;
    private final TaskMessageRepository messageRepository;
    private final PlaybookRepository playbookRepository;
    private final PlaybookStepRepository stepRepository;
    private final AgentRoleRepository roleRepository;
    private final Orchestrator orchestrator;
    private final ChatStreamService stream;

    /** BOSS 发布需求（默认立即启动协作） */
    @PostMapping
    public Result<Long> create(@RequestBody TaskCreateRequest req) {
        if (req.getTitle() == null || req.getTitle().isBlank()) {
            throw new BizException("需求标题必填");
        }
        if (req.getRequirement() == null || req.getRequirement().isBlank()) {
            throw new BizException("需求描述必填");
        }
        Playbook pb = req.getPlaybookId() != null
                ? playbookRepository.findById(req.getPlaybookId()).orElseThrow(() -> new BizException("剧本不存在"))
                : playbookRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new BizException("请先在【协作剧本】创建剧本"));

        Task task = new Task();
        task.setTitle(req.getTitle().strip());
        task.setRequirement(req.getRequirement().strip());
        task.setPlaybookId(pb.getId());
        task.setStatus(TaskStatus.CREATED);
        task.setCurrentStepOrder(1);
        task.setRound(1);
        task = taskRepository.save(task);

        bossMessage(task, "🧑‍💼 BOSS 发布需求「" + task.getTitle() + "」\n\n" + task.getRequirement());

        if (!Boolean.FALSE.equals(req.getStart())) {
            orchestrator.launch(task.getId());
        }
        return Result.ok(task.getId());
    }

    @GetMapping
    public Result<List<Task>> list() {
        return Result.ok(taskRepository.findByOrderByCreatedAtDesc());
    }

    @GetMapping("/{id}")
    public Result<TaskDetailVo> detail(@PathVariable Long id) {
        Task task = taskRepository.findById(id).orElseThrow(() -> new BizException("任务不存在"));
        Map<Long, AgentRole> roles = roleRepository.findAll().stream()
                .collect(Collectors.toMap(AgentRole::getId, Function.identity()));
        List<StepVo> steps = stepRepository.findByPlaybookIdOrderByStepOrderAsc(task.getPlaybookId())
                .stream().map(s -> {
                    AgentRole r = roles.get(s.getActorRoleId());
                    String state;
                    if (task.getStatus() == TaskStatus.DELIVERED || task.getStatus() == TaskStatus.ACCEPTED) {
                        state = "done";
                    } else if (s.getStepOrder() < task.getCurrentStepOrder()) {
                        state = "done";
                    } else if (s.getStepOrder().equals(task.getCurrentStepOrder())) {
                        state = task.getStatus() == TaskStatus.RUNNING ? "active" : "pending";
                    } else {
                        state = "pending";
                    }
                    return StepVo.of(s,
                            r == null ? "（角色缺失）" : r.getName(),
                            r == null ? "❓" : r.getEmoji(),
                            r == null ? "#909399" : r.getColor(),
                            state);
                }).toList();
        Playbook pb = playbookRepository.findById(task.getPlaybookId()).orElse(null);
        return Result.ok(TaskDetailVo.builder()
                .task(task)
                .messages(messageRepository.findByTaskIdOrderBySeqAsc(id))
                .steps(steps)
                .playbookId(task.getPlaybookId())
                .playbookName(pb == null ? "" : pb.getName())
                .maxRounds(pb == null ? 3 : pb.getMaxRounds())
                .build());
    }

    /** 启动 / 继续协作（任务创建未启动或失败恢复时使用） */
    @PostMapping("/{id}/launch")
    public Result<Void> launch(@PathVariable Long id) {
        orchestrator.launch(id);
        return Result.ok();
    }

    /** BOSS 采纳交付 */
    @PostMapping("/{id}/accept")
    public Result<Void> accept(@PathVariable Long id, @RequestBody(required = false) BossActionRequest req) {
        Task task = mustDelivered(id);
        task.setStatus(TaskStatus.ACCEPTED);
        task.setBossDecision("ACCEPT");
        task.setBossComment(req == null ? "" : req.getComment());
        taskRepository.save(task);
        bossMessage(task, "🧑‍💼 BOSS 已采纳本需求的交付。" + (req != null && req.getComment() != null && !req.getComment().isBlank()
                ? "验收意见：" + req.getComment() : ""));
        stream.push(id, "status", statusPayload(task));
        return Result.ok();
    }

    /** BOSS 退回交付：注入 BOSS 意见后从需求分析重新协作一轮 */
    @PostMapping("/{id}/reject")
    public Result<Void> reject(@PathVariable Long id, @RequestBody(required = false) BossActionRequest req) {
        Task task = mustDelivered(id);
        String comment = req == null || req.getComment() == null || req.getComment().isBlank()
                ? "整体不满意，请重新梳理并交付。" : req.getComment();
        bossMessage(task, "🧑‍💼 BOSS 退回了交付，意见：" + comment);
        task.setStatus(TaskStatus.RUNNING);
        task.setBossDecision("REJECT");
        task.setBossComment(comment);
        task.setCurrentStepOrder(1);
        task.setRound(1);
        task.setResult(null);
        taskRepository.save(task);
        stream.push(id, "status", statusPayload(task));
        orchestrator.launch(id);
        return Result.ok();
    }

    /** 删除任务（连同消息） */
    @DeleteMapping("/{id}")
    @Transactional
    public Result<Void> delete(@PathVariable Long id) {
        Task task = taskRepository.findById(id).orElseThrow(() -> new BizException("任务不存在"));
        if (task.getStatus() == TaskStatus.RUNNING) {
            throw new BizException("任务正在协作中，请等待完成或失败后再删除");
        }
        messageRepository.deleteByTaskId(id);
        taskRepository.deleteById(id);
        return Result.ok();
    }

    /** SSE 实时消息流 */
    @GetMapping(value = "/{id}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@PathVariable Long id) {
        taskRepository.findById(id).orElseThrow(() -> new BizException("任务不存在"));
        return stream.subscribe(id);
    }

    private Task mustDelivered(Long id) {
        Task task = taskRepository.findById(id).orElseThrow(() -> new BizException("任务不存在"));
        if (task.getStatus() != TaskStatus.DELIVERED) {
            throw new BizException("当前状态不可执行该操作（需为已交付）");
        }
        return task;
    }

    private void bossMessage(Task task, String content) {
        TaskMessage m = new TaskMessage();
        m.setTaskId(task.getId());
        m.setSeq(messageRepository.findMaxSeq(task.getId()) + 1);
        m.setSenderType(SenderType.BOSS);
        m.setSenderName("BOSS");
        m.setEmoji("🧑‍💼");
        m.setContent(content);
        m.setMsgType(MsgType.CHAT);
        messageRepository.save(m);
        stream.push(task.getId(), "message", m);
    }

    private Map<String, Object> statusPayload(Task task) {
        Map<String, Object> map = new HashMap<>();
        map.put("status", task.getStatus().name());
        map.put("currentStepOrder", task.getCurrentStepOrder());
        map.put("round", task.getRound());
        map.put("result", task.getResult() == null ? "" : task.getResult());
        return map;
    }
}

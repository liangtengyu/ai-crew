package com.aicrew.engine;

import com.aicrew.agent.PromptBuilder;
import com.aicrew.common.entity.AgentRole;
import com.aicrew.common.entity.Playbook;
import com.aicrew.common.entity.PlaybookStep;
import com.aicrew.common.entity.Task;
import com.aicrew.common.entity.TaskMessage;
import com.aicrew.common.enums.MsgType;
import com.aicrew.common.enums.SenderType;
import com.aicrew.common.enums.TaskStatus;
import com.aicrew.common.exception.BizException;
import com.aicrew.common.repository.AgentRoleRepository;
import com.aicrew.common.repository.PlaybookRepository;
import com.aicrew.common.repository.TaskMessageRepository;
import com.aicrew.common.repository.TaskRepository;
import com.aicrew.common.util.JsonUtil;
import com.aicrew.llm.LlmService;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 协作编排引擎：按剧本驱动 AI 角色轮转发言。
 * 非验收环节：发言后进入下一步骤；验收关卡：REJECT 跳回返工步骤并累加轮次，
 * 超过最大轮数强制交付；最后一个环节的发言即交付物。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class Orchestrator {

    /** 协作历史参与 prompt 的消息条数 */
    private static final int HISTORY_WINDOW = 12;
    private static final int HISTORY_SNIPPET_LEN = 600;
    private static final int HISTORY_FULL_LEN = 2000;

    private final TaskRepository taskRepository;
    private final TaskMessageRepository messageRepository;
    private final AgentRoleRepository roleRepository;
    private final PlaybookRepository playbookRepository;
    private final PlaybookService playbookService;
    private final LlmService llmService;
    private final PromptBuilder promptBuilder;
    private final ChatStreamService stream;

    private final ExecutorService pool = Executors.newFixedThreadPool(4, r -> {
        Thread t = new Thread(r, "agent-worker");
        t.setDaemon(true);
        return t;
    });

    /** 内存中的执行中任务，防止同一任务并发跑两份 */
    private final Set<Long> running = ConcurrentHashMap.newKeySet();

    /** 启动 / 恢复一个任务 */
    public void launch(Long taskId) {
        if (!running.add(taskId)) {
            throw new BizException("该任务正在执行中，请勿重复启动");
        }
        Task task = taskRepository.findById(taskId).orElseThrow(() -> new BizException("任务不存在"));
        if (task.getStatus() == TaskStatus.DELIVERED || task.getStatus() == TaskStatus.ACCEPTED) {
            running.remove(taskId);
            throw new BizException("该任务已交付，无法再次启动");
        }
        task.setStatus(TaskStatus.RUNNING);
        taskRepository.save(task);
        pushStatus(task);
        pool.submit(() -> {
            try {
                runLoop(taskId);
            } finally {
                running.remove(taskId);
            }
        });
    }

    private void runLoop(Long taskId) {
        try {
            loop(taskId);
        } catch (Exception e) {
            log.error("任务执行异常 taskId={}", taskId, e);
            taskRepository.findById(taskId).ifPresent(task -> {
                task.setStatus(TaskStatus.FAILED);
                taskRepository.save(task);
                systemMessage(task, "⚠️ 协作中断：" + e.getMessage() + "。可在上方点击「继续协作」重试当前环节。");
                pushStatus(task);
            });
        }
    }

    private void loop(Long taskId) {
        while (true) {
            Task task = taskRepository.findById(taskId).orElseThrow(() -> new BizException("任务不存在"));
            if (task.getStatus() != TaskStatus.RUNNING) {
                return; // 被外部终止/删除
            }
            List<PlaybookStep> steps = playbookService.steps(task.getPlaybookId());
            if (steps.isEmpty()) {
                throw new BizException("剧本没有任何环节，请到【协作剧本】配置");
            }
            int maxRounds = playbookRepository.findById(task.getPlaybookId())
                    .map(Playbook::getMaxRounds).orElse(3);

            PlaybookStep step = steps.stream()
                    .filter(s -> s.getStepOrder().equals(task.getCurrentStepOrder()))
                    .findFirst()
                    .orElse(steps.get(0));
            AgentRole role = roleRepository.findById(step.getActorRoleId())
                    .orElseThrow(() -> new BizException("环节「" + step.getName() + "」绑定的执行角色不存在"));
            if (!Boolean.TRUE.equals(role.getEnabled())) {
                throw new BizException("角色「" + role.getName() + "」已停用，请到【角色管理】启用");
            }

            // 1. 调用该角色绑定的模型
            String system = promptBuilder.systemPrompt(role);
            String user = promptBuilder.userPrompt(step, task, history(taskId), maxRounds,
                    Boolean.TRUE.equals(step.getRequireVerdict()));
            String raw = llmService.chat(role.getModelConfigId(), system, user);

            // 2. 解析结构化输出（解析失败则整段文本作为发言内容，避免卡死）
            JsonNode node = JsonUtil.extractJson(raw);
            String content = node != null && node.hasNonNull("content")
                    ? node.get("content").asText()
                    : raw;
            String verdict = null;
            if (Boolean.TRUE.equals(step.getRequireVerdict())) {
                verdict = node != null && node.hasNonNull("verdict")
                        ? node.get("verdict").asText("APPROVE").trim().toUpperCase()
                        : "APPROVE";
            }

            // 3. 落库 + SSE 推送
            TaskMessage message = roleMessage(task, role, step, content, verdict);

            // 4. 推进状态机
            boolean rejected = Boolean.TRUE.equals(step.getRequireVerdict()) && "REJECT".equals(verdict);
            if (rejected) {
                if (task.getRound() >= Math.max(maxRounds, 1)) {
                    systemMessage(task, "🔁 已达最大返工轮数（" + maxRounds + " 轮），按最新验收意见强制交付。");
                    deliver(task, content);
                    return;
                }
                task.setRound(task.getRound() + 1);
                task.setCurrentStepOrder(step.getOnRejectOrder() != null ? step.getOnRejectOrder() : step.getStepOrder());
                taskRepository.save(task);
                pushStatus(task);
                continue;
            }
            int next = step.getStepOrder() + 1;
            boolean hasNext = steps.stream().anyMatch(s -> s.getStepOrder() == next);
            if (!hasNext) {
                deliver(task, content);
                return;
            }
            task.setCurrentStepOrder(next);
            taskRepository.save(task);
            pushStatus(task);
        }
    }

    /** 交付：最后一个环节（或强制交付时最新验收意见）作为交付物 */
    private void deliver(Task task, String content) {
        task.setStatus(TaskStatus.DELIVERED);
        task.setResult(content);
        task.setDeliveryAt(LocalDateTime.now());
        taskRepository.save(task);
        systemMessage(task, "📦 AI 团队已完成交付，请 BOSS 在右侧「交付物」中查收，并可作出采纳 / 退回决定。");
        pushStatus(task);
    }

    /** 组装协作历史（窗口截断，最后一条保留更完整内容） */
    private String history(Long taskId) {
        List<TaskMessage> all = messageRepository.findByTaskIdOrderBySeqAsc(taskId);
        List<TaskMessage> tail = all.size() > HISTORY_WINDOW
                ? all.subList(all.size() - HISTORY_WINDOW, all.size())
                : all;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < tail.size(); i++) {
            TaskMessage m = tail.get(i);
            int limit = (i == tail.size() - 1) ? HISTORY_FULL_LEN : HISTORY_SNIPPET_LEN;
            sb.append("[").append(m.getSenderName());
            if (m.getTargetName() != null && !m.getTargetName().isBlank()) {
                sb.append(" → ").append(m.getTargetName());
            }
            sb.append("] ").append(truncate(m.getContent(), limit)).append("\n");
        }
        return sb.toString().strip();
    }

    private TaskMessage roleMessage(Task task, AgentRole role, PlaybookStep step, String content, String verdict) {
        TaskMessage m = new TaskMessage();
        m.setTaskId(task.getId());
        m.setSeq(nextSeq(task.getId()));
        m.setSenderType(SenderType.ROLE);
        m.setSenderRoleId(role.getId());
        m.setSenderName(role.getName());
        m.setEmoji(role.getEmoji());
        m.setTargetName(nextTarget(task, step));
        m.setContent(content);
        m.setMsgType(msgTypeOf(step.getCode()));
        m.setStepOrder(step.getStepOrder());
        m.setVerdict(verdict);
        m = messageRepository.save(m);
        stream.push(task.getId(), "message", m);
        return m;
    }

    private void systemMessage(Task task, String content) {
        TaskMessage m = new TaskMessage();
        m.setTaskId(task.getId());
        m.setSeq(nextSeq(task.getId()));
        m.setSenderType(SenderType.SYSTEM);
        m.setSenderName("系统");
        m.setEmoji("⚙️");
        m.setContent(content);
        m.setMsgType(MsgType.CHAT);
        messageRepository.save(m);
        stream.push(task.getId(), "message", m);
    }

    /** 发言对象：下一环节的执行角色；同一角色连续环节则发给「团队」；最后一个环节交给 BOSS */
    private String nextTarget(Task task, PlaybookStep step) {
        List<PlaybookStep> steps = playbookService.steps(task.getPlaybookId());
        PlaybookStep nextStep = steps.stream()
                .filter(s -> s.getStepOrder() > step.getStepOrder())
                .findFirst().orElse(null);
        if (nextStep == null) {
            return "BOSS";
        }
        if (nextStep.getActorRoleId().equals(step.getActorRoleId())) {
            return "团队";
        }
        return roleRepository.findById(nextStep.getActorRoleId())
                .map(AgentRole::getName).orElse("BOSS");
    }

    private MsgType msgTypeOf(String code) {
        if (code == null) {
            return MsgType.CHAT;
        }
        return switch (code) {
            case "SOLUTION_DESIGN" -> MsgType.DESIGN;
            case "DEVELOPMENT" -> MsgType.IMPLEMENT;
            case "ACCEPTANCE" -> MsgType.REVIEW;
            case "DELIVERY" -> MsgType.DELIVERY;
            default -> MsgType.CHAT;
        };
    }

    private long nextSeq(Long taskId) {
        return messageRepository.findMaxSeq(taskId) + 1;
    }

    private void pushStatus(Task task) {
        stream.push(task.getId(), "status", Map.of(
                "status", task.getStatus().name(),
                "currentStepOrder", task.getCurrentStepOrder() == null ? 1 : task.getCurrentStepOrder(),
                "round", task.getRound() == null ? 1 : task.getRound(),
                "result", task.getResult() == null ? "" : task.getResult()));
    }

    private String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max) + "…";
    }

    @PreDestroy
    public void shutdown() {
        pool.shutdownNow();
    }
}

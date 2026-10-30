package com.aicrew.server.controller;

import com.aicrew.common.dto.Result;
import com.aicrew.common.entity.AgentRole;
import com.aicrew.common.entity.Playbook;
import com.aicrew.common.entity.PlaybookStep;
import com.aicrew.common.exception.BizException;
import com.aicrew.common.repository.AgentRoleRepository;
import com.aicrew.common.repository.PlaybookRepository;
import com.aicrew.common.repository.PlaybookStepRepository;
import com.aicrew.server.dto.PlaybookUpdateRequest;
import com.aicrew.server.dto.PlaybookVo;
import com.aicrew.server.dto.StepUpsertRequest;
import com.aicrew.server.dto.StepVo;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** 协作剧本管理：查看 / 编辑元信息 / 步骤增删改 / 上移下移 */
@RestController
@RequestMapping("/api/playbooks")
@RequiredArgsConstructor
public class PlaybookController {

    private final PlaybookRepository playbookRepository;
    private final PlaybookStepRepository stepRepository;
    private final AgentRoleRepository roleRepository;

    @GetMapping
    public Result<List<PlaybookVo>> list() {
        Map<Long, AgentRole> roles = roleRepository.findAll().stream()
                .collect(Collectors.toMap(AgentRole::getId, Function.identity()));
        return Result.ok(playbookRepository.findAll().stream()
                .map(pb -> PlaybookVo.of(pb, steps(pb.getId(), roles, null)))
                .toList());
    }

    private List<StepVo> steps(Long playbookId, Map<Long, AgentRole> roles, Integer activeOrder) {
        List<PlaybookStep> list = stepRepository.findByPlaybookIdOrderByStepOrderAsc(playbookId);
        return list.stream().map(s -> {
            AgentRole r = roles.get(s.getActorRoleId());
            String state = activeOrder == null ? "pending"
                    : s.getStepOrder() < activeOrder ? "done"
                    : s.getStepOrder().equals(activeOrder) ? "active" : "pending";
            return StepVo.of(s,
                    r == null ? "（角色缺失）" : r.getName(),
                    r == null ? "❓" : r.getEmoji(),
                    r == null ? "#909399" : r.getColor(),
                    state);
        }).toList();
    }

    @PutMapping("/{id}")
    public Result<Playbook> update(@PathVariable Long id, @RequestBody PlaybookUpdateRequest req) {
        Playbook pb = playbookRepository.findById(id).orElseThrow(() -> new BizException("剧本不存在"));
        if (req.getName() != null && !req.getName().isBlank()) {
            pb.setName(req.getName());
        }
        pb.setDescription(req.getDescription());
        if (req.getMaxRounds() != null) {
            pb.setMaxRounds(Math.max(1, req.getMaxRounds()));
        }
        return Result.ok(playbookRepository.save(pb));
    }

    @PostMapping("/{id}/steps")
    @Transactional
    public Result<PlaybookVo> addStep(@PathVariable Long id, @RequestBody StepUpsertRequest req) {
        Playbook pb = playbookRepository.findById(id).orElseThrow(() -> new BizException("剧本不存在"));
        validateStep(req);
        PlaybookStep s = new PlaybookStep();
        apply(s, req);
        s.setPlaybookId(id);
        s.setStepOrder(stepRepository.findByPlaybookIdOrderByStepOrderAsc(id).stream()
                .mapToInt(PlaybookStep::getStepOrder).max().orElse(0) + 1);
        s.setDeliverAfter(false);
        stepRepository.save(s);
        return Result.ok(getOne(id));
    }

    @PutMapping("/steps/{stepId}")
    @Transactional
    public Result<PlaybookVo> updateStep(@PathVariable Long stepId, @RequestBody StepUpsertRequest req) {
        PlaybookStep s = stepRepository.findById(stepId).orElseThrow(() -> new BizException("步骤不存在"));
        validateStep(req);
        apply(s, req);
        stepRepository.save(s);
        return Result.ok(getOne(s.getPlaybookId()));
    }

    @DeleteMapping("/steps/{stepId}")
    @Transactional
    public Result<PlaybookVo> deleteStep(@PathVariable Long stepId) {
        PlaybookStep s = stepRepository.findById(stepId).orElseThrow(() -> new BizException("步骤不存在"));
        stepRepository.delete(s);
        stepRepository.flush();
        // 收紧序号为连续 1..n，避免出现空档导致提前交付
        List<PlaybookStep> rest = stepRepository.findByPlaybookIdOrderByStepOrderAsc(s.getPlaybookId());
        int order = 1;
        for (PlaybookStep ps : rest) {
            ps.setStepOrder(order++);
            stepRepository.save(ps);
        }
        return Result.ok(getOne(s.getPlaybookId()));
    }

    @PostMapping("/steps/{stepId}/move")
    @Transactional
    public Result<PlaybookVo> move(@PathVariable Long stepId, @RequestParam String dir) {
        PlaybookStep s = stepRepository.findById(stepId).orElseThrow(() -> new BizException("步骤不存在"));
        List<PlaybookStep> all = stepRepository.findByPlaybookIdOrderByStepOrderAsc(s.getPlaybookId());
        int idx = all.indexOf(s);
        int target = "up".equals(dir) ? idx - 1 : idx + 1;
        if (target < 0 || target >= all.size()) {
            throw new BizException("已无法继续移动");
        }
        PlaybookStep other = all.get(target);
        int tmp = s.getStepOrder();
        s.setStepOrder(other.getStepOrder());
        other.setStepOrder(tmp);
        stepRepository.save(s);
        stepRepository.save(other);
        return Result.ok(getOne(s.getPlaybookId()));
    }

    private PlaybookVo getOne(Long playbookId) {
        Map<Long, AgentRole> roles = roleRepository.findAll().stream()
                .collect(Collectors.toMap(AgentRole::getId, Function.identity()));
        Playbook pb = playbookRepository.findById(playbookId).orElseThrow();
        return PlaybookVo.of(pb, steps(playbookId, roles, null));
    }

    private void apply(PlaybookStep s, StepUpsertRequest req) {
        s.setCode(req.getCode() == null || req.getCode().isBlank()
                ? "STEP_" + System.currentTimeMillis() % 100000 : req.getCode().trim().toUpperCase());
        s.setName(req.getName());
        s.setActorRoleId(req.getActorRoleId());
        s.setPromptTemplate(req.getPromptTemplate());
        s.setRequireVerdict(Boolean.TRUE.equals(req.getRequireVerdict()));
        s.setOnRejectOrder(req.getOnRejectOrder());
    }

    private void validateStep(StepUpsertRequest req) {
        if (req.getName() == null || req.getName().isBlank()) {
            throw new BizException("环节名称必填");
        }
        if (req.getActorRoleId() == null) {
            throw new BizException("请选择执行角色");
        }
        if (req.getPromptTemplate() == null || req.getPromptTemplate().isBlank()) {
            throw new BizException("环节指令模板必填");
        }
    }
}

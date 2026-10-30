package com.aicrew.server.controller;

import com.aicrew.common.dto.Result;
import com.aicrew.common.entity.AgentRole;
import com.aicrew.common.entity.ModelConfig;
import com.aicrew.common.exception.BizException;
import com.aicrew.common.repository.AgentRoleRepository;
import com.aicrew.common.repository.ModelConfigRepository;
import com.aicrew.common.repository.PlaybookStepRepository;
import com.aicrew.server.dto.RoleVo;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** AI 角色 CRUD */
@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class AgentRoleController {

    private final AgentRoleRepository roleRepository;
    private final ModelConfigRepository modelRepository;
    private final PlaybookStepRepository stepRepository;

    @GetMapping
    public Result<List<RoleVo>> list() {
        Map<Long, ModelConfig> models = modelRepository.findAll().stream()
                .collect(Collectors.toMap(ModelConfig::getId, Function.identity()));
        return Result.ok(roleRepository.findAll().stream()
                .map(r -> RoleVo.of(r, modelName(models, r.getModelConfigId())))
                .toList());
    }

    private String modelName(Map<Long, ModelConfig> models, Long id) {
        ModelConfig m = id == null ? null : models.get(id);
        return m == null ? "（未绑定）" : m.getName();
    }

    @PostMapping
    public Result<AgentRole> create(@RequestBody AgentRole role) {
        validate(role);
        role.setId(null);
        if (role.getEmoji() == null || role.getEmoji().isBlank()) {
            role.setEmoji(role.getRoleType().defaultEmoji);
        }
        return Result.ok(roleRepository.save(role));
    }

    @PutMapping("/{id}")
    public Result<AgentRole> update(@PathVariable Long id, @RequestBody AgentRole role) {
        AgentRole old = roleRepository.findById(id).orElseThrow(() -> new BizException("角色不存在"));
        validate(role);
        old.setName(role.getName());
        old.setRoleType(role.getRoleType());
        old.setEmoji(role.getEmoji());
        old.setColor(role.getColor());
        old.setSystemPrompt(role.getSystemPrompt());
        old.setModelConfigId(role.getModelConfigId());
        old.setEnabled(role.getEnabled());
        return Result.ok(roleRepository.save(old));
    }

    @DeleteMapping("/{id}")
    @Transactional
    public Result<Void> delete(@PathVariable Long id) {
        boolean used = stepRepository.findAll().stream()
                .anyMatch(s -> id.equals(s.getActorRoleId()));
        if (used) {
            throw new BizException("该角色仍被协作剧本引用，请先到【协作剧本】调整后再删除");
        }
        roleRepository.deleteById(id);
        return Result.ok();
    }

    private void validate(AgentRole role) {
        if (role.getName() == null || role.getName().isBlank()) {
            throw new BizException("角色名必填");
        }
        if (role.getRoleType() == null) {
            throw new BizException("请选择角色类型");
        }
        if (role.getModelConfigId() == null) {
            throw new BizException("请为角色绑定模型");
        }
    }
}

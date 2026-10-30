package com.aicrew.server.controller;

import com.aicrew.common.dto.Result;
import com.aicrew.common.entity.ModelConfig;
import com.aicrew.common.exception.BizException;
import com.aicrew.common.repository.AgentRoleRepository;
import com.aicrew.common.repository.ModelConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 模型配置 CRUD */
@RestController
@RequestMapping("/api/models")
@RequiredArgsConstructor
public class ModelConfigController {

    private final ModelConfigRepository modelRepository;
    private final AgentRoleRepository roleRepository;

    @GetMapping
    public Result<List<ModelConfig>> list() {
        return Result.ok(modelRepository.findAll());
    }

    @PostMapping
    public Result<ModelConfig> create(@RequestBody ModelConfig cfg) {
        if (cfg.getName() == null || cfg.getName().isBlank()
                || cfg.getBaseUrl() == null || cfg.getBaseUrl().isBlank()
                || cfg.getModel() == null || cfg.getModel().isBlank()) {
            throw new BizException("名称 / Base URL / 模型 ID 均必填");
        }
        cfg.setId(null);
        return Result.ok(modelRepository.save(cfg));
    }

    @PutMapping("/{id}")
    public Result<ModelConfig> update(@PathVariable Long id, @RequestBody ModelConfig cfg) {
        ModelConfig old = modelRepository.findById(id).orElseThrow(() -> new BizException("模型配置不存在"));
        old.setName(cfg.getName());
        old.setBaseUrl(cfg.getBaseUrl());
        old.setApiKey(cfg.getApiKey());
        old.setModel(cfg.getModel());
        old.setTemperature(cfg.getTemperature());
        old.setMaxTokens(cfg.getMaxTokens());
        old.setEnabled(cfg.getEnabled());
        return Result.ok(modelRepository.save(old));
    }

    @DeleteMapping("/{id}")
    @Transactional
    public Result<Void> delete(@PathVariable Long id) {
        boolean used = roleRepository.findAll().stream()
                .anyMatch(r -> id.equals(r.getModelConfigId()));
        if (used) {
            throw new BizException("该模型仍被角色绑定，请先到【角色管理】换绑后再删除");
        }
        modelRepository.deleteById(id);
        return Result.ok();
    }
}

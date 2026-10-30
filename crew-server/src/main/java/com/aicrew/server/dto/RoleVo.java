package com.aicrew.server.dto;

import com.aicrew.common.entity.AgentRole;
import com.aicrew.common.enums.RoleType;
import lombok.Builder;
import lombok.Data;

/** 角色视图（带绑定的模型名） */
@Data
@Builder
public class RoleVo {
    private Long id;
    private String name;
    private RoleType roleType;
    private String emoji;
    private String color;
    private String systemPrompt;
    private Long modelConfigId;
    private String modelName;
    private Boolean enabled;

    public static RoleVo of(AgentRole r, String modelName) {
        return RoleVo.builder()
                .id(r.getId())
                .name(r.getName())
                .roleType(r.getRoleType())
                .emoji(r.getEmoji())
                .color(r.getColor())
                .systemPrompt(r.getSystemPrompt())
                .modelConfigId(r.getModelConfigId())
                .modelName(modelName)
                .enabled(r.getEnabled())
                .build();
    }
}

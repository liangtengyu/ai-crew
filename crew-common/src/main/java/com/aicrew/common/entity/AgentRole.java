package com.aicrew.common.entity;

import com.aicrew.common.enums.RoleType;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/** AI 角色人设：一个虚拟团队成员，绑定一个模型配置 */
@Data
@Entity
@Table(name = "agent_role")
public class AgentRole {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 角色名，如「产品经理·艾玛」 */
    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RoleType roleType;

    /** 头像 emoji */
    @Column(length = 16)
    private String emoji;

    /** 主题色，如 #409EFF */
    @Column(length = 16)
    private String color;

    /** 人设提示词，留空则使用内置默认人设 */
    @Column(length = 4000)
    private String systemPrompt;

    /** 绑定的模型配置 ID */
    @Column(name = "model_config_id")
    private Long modelConfigId;

    private Boolean enabled = true;

    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}

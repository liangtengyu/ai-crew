package com.aicrew.common.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/** 模型配置：一个 OpenAI 兼容的大模型端点（DeepSeek / GLM / Qwen / OpenAI...） */
@Data
@Entity
@Table(name = "model_config")
public class ModelConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 展示名，如「DeepSeek 官方」 */
    @Column(nullable = false)
    private String name;

    /** OpenAI 兼容 Base URL，要求 {base}/chat/completions 可访问 */
    @Column(nullable = false)
    private String baseUrl;

    private String apiKey;

    /** 模型 ID，如 deepseek-chat / glm-4-flash */
    @Column(nullable = false)
    private String model;

    private Double temperature = 0.7;

    private Integer maxTokens = 4096;

    private Boolean enabled = true;

    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}

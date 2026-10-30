package com.aicrew.common.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/** 协作剧本：定义一个需求从提出到交付要经过哪些环节、每个环节由谁执行 */
@Data
@Entity
@Table(name = "playbook")
public class Playbook {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(length = 500)
    private String description;

    /** 验收驳回后的最大返工轮数 */
    private Integer maxRounds = 3;

    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}

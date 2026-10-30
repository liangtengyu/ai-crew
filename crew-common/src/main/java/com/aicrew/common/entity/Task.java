package com.aicrew.common.entity;

import com.aicrew.common.enums.TaskStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/** 需求任务：BOSS 提出的一个需求，走完剧本后交付 */
@Data
@Entity
@Table(name = "task")
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    /** 需求描述（BOSS 原话） */
    @Column(nullable = false, length = 8000)
    private String requirement;

    @Column(name = "playbook_id", nullable = false)
    private Long playbookId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TaskStatus status = TaskStatus.CREATED;

    /** 当前执行到的剧本步骤序号 */
    @Column(name = "current_step_order")
    private Integer currentStepOrder = 1;

    /** 当前返工轮次 */
    private Integer round = 1;

    /** 最终交付物（最后一环节的发言） */
    @Column(length = 20000)
    private String result;

    /** BOSS 验收意见 */
    private String bossComment;

    /** BOSS 决定：ACCEPT / REJECT */
    @Column(name = "boss_decision", length = 20)
    private String bossDecision;

    private LocalDateTime deliveryAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

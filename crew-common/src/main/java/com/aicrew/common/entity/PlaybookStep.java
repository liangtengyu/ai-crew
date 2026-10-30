package com.aicrew.common.entity;

import jakarta.persistence.*;
import lombok.Data;

/** 剧本环节：步骤按 stepOrder 顺序执行；验收关卡驳回时跳回 onRejectOrder 对应步骤返工 */
@Data
@Entity
@Table(name = "playbook_step")
public class PlaybookStep {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "playbook_id", nullable = false)
    private Long playbookId;

    /** 步骤序号，从 1 开始 */
    @Column(name = "step_order", nullable = false)
    private Integer stepOrder;

    /** 环节编码，如 REQUIREMENT_ANALYSIS / SOLUTION_DESIGN / DEVELOPMENT / ACCEPTANCE / DELIVERY */
    @Column(nullable = false, length = 50)
    private String code;

    /** 环节名，如「方案设计」 */
    @Column(nullable = false, length = 100)
    private String name;

    /** 该环节的任务指令模板，支持占位符 {title} {requirement} {history} {round} {maxRounds} */
    @Column(nullable = false, length = 8000)
    private String promptTemplate;

    /** 执行该环节的角色 ID */
    @Column(name = "actor_role_id", nullable = false)
    private Long actorRoleId;

    /** 是否为验收关卡（模型必须给出 APPROVE / REJECT） */
    @Column(name = "require_verdict")
    private Boolean requireVerdict = false;

    /** 驳回后跳回的步骤序号（仅验收关卡生效，空则原地重跑） */
    @Column(name = "on_reject_order")
    private Integer onRejectOrder;

    /** 由此环节结束后交付给 BOSS（最后一个环节默认交付，无需配置） */
    private Boolean deliverAfter = false;
}

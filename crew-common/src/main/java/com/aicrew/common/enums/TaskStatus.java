package com.aicrew.common.enums;

/** 任务状态 */
public enum TaskStatus {
    /** 已创建未启动 */
    CREATED,
    /** 协作进行中 */
    RUNNING,
    /** AI 团队已交付，等待 BOSS 验收 */
    DELIVERED,
    /** BOSS 已采纳 */
    ACCEPTED,
    /** 执行失败（模型调用失败等），可继续 */
    FAILED
}

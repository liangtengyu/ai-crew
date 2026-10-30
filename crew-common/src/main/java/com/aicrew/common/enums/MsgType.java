package com.aicrew.common.enums;

/** 消息类型（用于前端区分气泡样式） */
public enum MsgType {
    /** 普通发言 */
    CHAT,
    /** 方案设计 */
    DESIGN,
    /** 开发实现 */
    IMPLEMENT,
    /** 验收评审 */
    REVIEW,
    /** 最终交付 */
    DELIVERY,
    /** 系统/错误提示 */
    ERROR
}

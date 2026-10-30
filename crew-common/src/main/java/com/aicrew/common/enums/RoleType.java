package com.aicrew.common.enums;

/** 角色类型（决定默认人设模板，可自定义扩展） */
public enum RoleType {
    /** 产品经理 */
    PM("产品经理", "🧭"),
    /** 开发工程师 */
    DEV("开发工程师", "👨‍💻"),
    /** 测试工程师 */
    TEST("测试工程师", "🧪"),
    /** 架构师 */
    ARCHITECT("架构师", "🏛️"),
    /** 业务分析师 */
    BA("业务分析师", "📊"),
    /** UI 设计师 */
    UI("UI 设计师", "🎨"),
    /** 自定义 */
    CUSTOM("自定义角色", "🤖");

    public final String label;
    public final String defaultEmoji;

    RoleType(String label, String defaultEmoji) {
        this.label = label;
        this.defaultEmoji = defaultEmoji;
    }
}

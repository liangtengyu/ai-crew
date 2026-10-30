package com.aicrew.agent;

import com.aicrew.common.entity.AgentRole;
import com.aicrew.common.entity.PlaybookStep;
import com.aicrew.common.entity.Task;
import org.springframework.stereotype.Component;

/** Prompt 组装器：人设 system + 环节指令 + 团队上下文 + 输出格式约束 */
@Component
public class PromptBuilder {

    private static final String COLLAB_RULES = """
                你是「AI Crew · AI 虚拟协作团队」的成员，正在一个协作看板上与队友沟通。
                要求：始终使用简体中文；发言直接给结论与干货，不要客套；内容会展示给 BOSS 查看。""";

    /** 角色人设 system prompt：优先使用自定义人设，否则用内置模板 */
    public String systemPrompt(AgentRole role) {
        String persona = (role.getSystemPrompt() != null && !role.getSystemPrompt().isBlank())
                ? role.getSystemPrompt()
                : PersonaDefaults.of(role.getRoleType(), role.getName());
        return persona.strip() + "\n\n" + COLLAB_RULES;
    }

    /** 环节指令 user prompt */
    public String userPrompt(PlaybookStep step, Task task, String history, int maxRounds, boolean gate) {
        String tpl = fill(step.getPromptTemplate(), task, history, maxRounds);

        StringBuilder sb = new StringBuilder();
        sb.append(tpl == null || tpl.isBlank() ? "请完成环节「" + step.getName() + "」。" : tpl.strip());
        sb.append("\n\n── 团队上下文 ──")
          .append("\n【BOSS 需求】").append(safe(task.getTitle())).append("：").append(safe(task.getRequirement()))
          .append("\n【协作历史】(由早到晚，" ).append(history.isBlank() ? "暂无" : "节选").append(")\n")
          .append(history.isBlank() ? "（协作刚开始，没有历史消息）" : history)
          .append("\n【当前】第 ").append(task.getRound()).append("/").append(Math.max(maxRounds, 1))
          .append(" 轮 · 当前环节：").append(step.getName())
          .append(" · 执行角色：").append("你");

        sb.append("\n\n── 输出格式（必须严格遵守）──")
          .append("\n只输出一个 JSON 对象，禁止使用代码块围栏，禁止输出 JSON 以外的任何文字：")
          .append("\n{\"content\":\"你的完整发言内容（支持换行 \\n 与 markdown）\"}");
        if (gate) {
            sb.append("\n本环节是验收关卡，必须额外给出结论 verdict，只能取 APPROVE（通过）或 REJECT（打回返工）：")
              .append("\n{\"content\":\"验收意见……\",\"verdict\":\"APPROVE\"}");
        }
        return sb.toString();
    }

    private String fill(String tpl, Task task, String history, int maxRounds) {
        if (tpl == null) {
            return "";
        }
        return tpl.replace("{title}", safe(task.getTitle()))
                .replace("{requirement}", safe(task.getRequirement()))
                .replace("{history}", history)
                .replace("{round}", String.valueOf(task.getRound()))
                .replace("{maxRounds}", String.valueOf(Math.max(maxRounds, 1)));
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }
}

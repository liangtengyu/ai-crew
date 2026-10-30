package com.aicrew.server.dto;

import com.aicrew.common.entity.PlaybookStep;
import lombok.Builder;
import lombok.Data;

/** 剧本步骤展示视图（带执行角色信息与执行状态） */
@Data
@Builder
public class StepVo {
    private Long id;
    private Integer stepOrder;
    private String name;
    private String code;
    private Long actorRoleId;
    private String actorName;
    private String emoji;
    private String color;
    private Boolean requireVerdict;
    private Integer onRejectOrder;
    private String promptTemplate;
    /** done / active / pending */
    private String state;

    public static StepVo of(PlaybookStep s, String actorName, String emoji, String color, String state) {
        return StepVo.builder()
                .id(s.getId())
                .stepOrder(s.getStepOrder())
                .name(s.getName())
                .code(s.getCode())
                .actorRoleId(s.getActorRoleId())
                .actorName(actorName)
                .emoji(emoji)
                .color(color)
                .requireVerdict(s.getRequireVerdict())
                .onRejectOrder(s.getOnRejectOrder())
                .promptTemplate(s.getPromptTemplate())
                .state(state)
                .build();
    }
}

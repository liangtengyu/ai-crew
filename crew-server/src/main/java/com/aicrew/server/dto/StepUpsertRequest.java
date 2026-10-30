package com.aicrew.server.dto;

import lombok.Data;

/** 剧本步骤新增 / 编辑请求 */
@Data
public class StepUpsertRequest {
    private String code;
    private String name;
    private Long actorRoleId;
    private String promptTemplate;
    private Boolean requireVerdict;
    private Integer onRejectOrder;
}

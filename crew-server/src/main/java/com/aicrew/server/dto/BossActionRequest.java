package com.aicrew.server.dto;

import lombok.Data;

/** BOSS 验收动作请求 */
@Data
public class BossActionRequest {
    /** 验收意见（可选） */
    private String comment;
}

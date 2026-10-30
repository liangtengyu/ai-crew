package com.aicrew.server.dto;

import lombok.Data;

/** 剧本元信息更新请求 */
@Data
public class PlaybookUpdateRequest {
    private String name;
    private String description;
    private Integer maxRounds;
}

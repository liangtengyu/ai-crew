package com.aicrew.server.dto;

import lombok.Data;

/** 创建需求请求 */
@Data
public class TaskCreateRequest {
    private String title;
    private String requirement;
    /** 协作剧本，空则使用默认第一个 */
    private Long playbookId;
    /** 创建后立即启动协作，默认 true */
    private Boolean start = true;
}

package com.aicrew.server.dto;

import com.aicrew.common.entity.Task;
import com.aicrew.common.entity.TaskMessage;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/** 作战室详情视图 */
@Data
@Builder
public class TaskDetailVo {
    private Task task;
    private List<TaskMessage> messages;
    private List<StepVo> steps;
    private Long playbookId;
    private String playbookName;
    private Integer maxRounds;
}

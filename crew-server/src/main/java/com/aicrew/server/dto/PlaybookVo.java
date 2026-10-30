package com.aicrew.server.dto;

import com.aicrew.common.entity.Playbook;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/** 剧本视图（含步骤） */
@Data
@Builder
public class PlaybookVo {
    private Long id;
    private String name;
    private String description;
    private Integer maxRounds;
    private List<StepVo> steps;

    public static PlaybookVo of(Playbook pb, List<StepVo> steps) {
        return PlaybookVo.builder()
                .id(pb.getId())
                .name(pb.getName())
                .description(pb.getDescription())
                .maxRounds(pb.getMaxRounds())
                .steps(steps)
                .build();
    }
}

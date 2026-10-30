package com.aicrew.engine;

import com.aicrew.common.entity.PlaybookStep;
import com.aicrew.common.repository.PlaybookRepository;
import com.aicrew.common.repository.PlaybookStepRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/** 剧本服务 */
@Service
@RequiredArgsConstructor
public class PlaybookService {

    private final PlaybookRepository playbookRepository;
    private final PlaybookStepRepository stepRepository;

    public List<PlaybookStep> steps(Long playbookId) {
        return stepRepository.findByPlaybookIdOrderByStepOrderAsc(playbookId);
    }
}

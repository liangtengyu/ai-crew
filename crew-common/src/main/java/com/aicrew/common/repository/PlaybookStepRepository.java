package com.aicrew.common.repository;

import com.aicrew.common.entity.PlaybookStep;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlaybookStepRepository extends JpaRepository<PlaybookStep, Long> {
    List<PlaybookStep> findByPlaybookIdOrderByStepOrderAsc(Long playbookId);

    void deleteByPlaybookId(Long playbookId);
}

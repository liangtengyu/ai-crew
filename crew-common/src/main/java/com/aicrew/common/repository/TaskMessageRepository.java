package com.aicrew.common.repository;

import com.aicrew.common.entity.TaskMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TaskMessageRepository extends JpaRepository<TaskMessage, Long> {

    List<TaskMessage> findByTaskIdOrderBySeqAsc(Long taskId);

    void deleteByTaskId(Long taskId);

    @Query("select coalesce(max(m.seq), 0) from TaskMessage m where m.taskId = :taskId")
    long findMaxSeq(@Param("taskId") Long taskId);
}

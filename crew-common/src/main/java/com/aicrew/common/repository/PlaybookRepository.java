package com.aicrew.common.repository;

import com.aicrew.common.entity.Playbook;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlaybookRepository extends JpaRepository<Playbook, Long> {
}

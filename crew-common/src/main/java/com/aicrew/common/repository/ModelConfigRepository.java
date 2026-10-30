package com.aicrew.common.repository;

import com.aicrew.common.entity.ModelConfig;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ModelConfigRepository extends JpaRepository<ModelConfig, Long> {
}

package com.example.machines.prediction;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PredictionRepository extends JpaRepository<PredictionEntity, Long> {
    List<PredictionEntity> findTop20ByOrderByCreatedAtDesc();
}

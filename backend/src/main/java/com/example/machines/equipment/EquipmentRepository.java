package com.example.machines.equipment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EquipmentRepository extends JpaRepository<EquipmentEntity, String> {
    List<EquipmentEntity> findAllByCategoryIgnoreCaseOrderByManufacturerAscModelAsc(String category);
}

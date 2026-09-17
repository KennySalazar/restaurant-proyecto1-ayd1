package com.restaurante.domain.repository;

import com.restaurante.domain.model.MeasurementUnit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MeasurementUnitRepository extends JpaRepository<MeasurementUnit, Short> {

    List<MeasurementUnit> findByActiveTrueOrderByNameAsc();

    Optional<MeasurementUnit> findByIdAndActiveTrue(Short id);
}

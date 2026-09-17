package com.restaurante.domain.repository;

import com.restaurante.domain.model.TableZone;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TableZoneRepository extends JpaRepository<TableZone, Long> {

    Optional<TableZone> findByIdAndRestaurantIdAndActiveTrue(Long id, Long restaurantId);

    List<TableZone> findAllByRestaurantIdAndActiveTrueOrderByIdAsc(Long restaurantId);
}
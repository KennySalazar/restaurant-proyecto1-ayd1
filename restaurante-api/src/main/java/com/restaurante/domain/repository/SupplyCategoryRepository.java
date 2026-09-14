package com.restaurante.domain.repository;

import com.restaurante.domain.model.SupplyCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SupplyCategoryRepository extends JpaRepository<SupplyCategory, Long> {

    List<SupplyCategory> findByRestaurantIdAndActiveTrueOrderByNameAsc(Long restaurantId);

    Optional<SupplyCategory> findByIdAndRestaurantIdAndActiveTrue(Long id, Long restaurantId);
}

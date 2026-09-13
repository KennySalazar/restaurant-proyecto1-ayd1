package com.restaurante.domain.repository;

import com.restaurante.domain.model.Supply;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SupplyRepository extends JpaRepository<Supply, Long> {

    boolean existsByRestaurantIdAndNameIgnoreCase(Long restaurantId, String name);

    boolean existsByRestaurantIdAndCodeIgnoreCase(Long restaurantId, String code);

    long countByRestaurantId(Long restaurantId);

    @EntityGraph(attributePaths = { "category", "measurementUnit" })
    List<Supply> findByRestaurantIdAndActiveTrueOrderByCreatedAtDesc(Long restaurantId);

    @EntityGraph(attributePaths = { "category", "measurementUnit" })
    Optional<Supply> findByIdAndRestaurantId(Long id, Long restaurantId);
}

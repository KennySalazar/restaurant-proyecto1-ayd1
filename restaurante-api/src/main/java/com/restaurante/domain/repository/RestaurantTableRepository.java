package com.restaurante.domain.repository;

import com.restaurante.domain.model.RestaurantTable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RestaurantTableRepository extends JpaRepository<RestaurantTable, Long> {

    boolean existsByRestaurantIdAndNumber(Long restaurantId, String number);

    boolean existsByRestaurantIdAndNumberAndIdNot(
            Long restaurantId,
            String number,
            Long id
    );

    Optional<RestaurantTable> findByIdAndRestaurantIdAndActiveTrue(
            Long id,
            Long restaurantId
    );
}
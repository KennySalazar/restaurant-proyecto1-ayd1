package com.restaurante.domain.repository;

import com.restaurante.domain.model.RestaurantTable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

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

    List<RestaurantTable> findAllByRestaurantIdOrderByIdAsc(Long restaurantId);

    Optional<RestaurantTable> findByIdAndRestaurantId(
            Long id,
            Long restaurantId
    );
}
package com.restaurante.domain.repository;

import com.restaurante.domain.model.RestaurantTable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RestaurantTableRepository extends JpaRepository<RestaurantTable, Long> {

    boolean existsByRestaurantIdAndNumber(Long restaurantId, String number);
}
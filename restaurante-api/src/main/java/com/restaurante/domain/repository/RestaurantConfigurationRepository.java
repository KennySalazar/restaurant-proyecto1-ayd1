package com.restaurante.domain.repository;

import com.restaurante.domain.model.RestaurantConfiguration;
import com.restaurante.domain.model.RestaurantConfigurationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RestaurantConfigurationRepository
        extends JpaRepository<RestaurantConfiguration, Long> {

    Optional<RestaurantConfiguration>
    findByRestaurantIdAndStatus(
            Long restaurantId,
            RestaurantConfigurationStatus status
    );
}
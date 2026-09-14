package com.restaurante.domain.repository;

import com.restaurante.domain.model.RestaurantUserProfile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RestaurantUserProfileRepository
        extends JpaRepository<RestaurantUserProfile, Long> {
}

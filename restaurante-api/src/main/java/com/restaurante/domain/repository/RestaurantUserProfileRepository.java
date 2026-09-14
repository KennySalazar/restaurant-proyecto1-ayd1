package com.restaurante.domain.repository;

import com.restaurante.domain.model.RestaurantUserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface RestaurantUserProfileRepository
        extends JpaRepository<RestaurantUserProfile, Long> {

    boolean existsByRestaurantIdAndEmployeeCode(
            Long restaurantId,
            String employeeCode
    );

    boolean existsByRestaurantIdAndEmployeeCodeAndIdNot(
            Long restaurantId,
            String employeeCode,
            Long id
    );

    List<RestaurantUserProfile> findAllByRestaurantIdOrderByIdAsc(
            Long restaurantId
    );

    Optional<RestaurantUserProfile> findByIdAndRestaurantId(
            Long id,
            Long restaurantId
    );
}
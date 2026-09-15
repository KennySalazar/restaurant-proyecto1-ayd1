package com.restaurante.domain.repository;

import com.restaurante.domain.model.RestaurantUserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    @Query("SELECT u.id FROM RestaurantUserProfile u JOIN UserAccount au ON au.id = u.id WHERE u.restaurantId = :restaurantId AND au.role.name = com.restaurante.domain.model.RoleName.WAITER AND au.enabled = true ORDER BY u.id ASC")
    List<Long> findActiveWaitersByRestaurantId(@Param("restaurantId") Long restaurantId);
}
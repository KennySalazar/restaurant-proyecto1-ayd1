package com.restaurante.domain.repository;

import com.restaurante.domain.model.Dish;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface DishRepository extends JpaRepository<Dish, Long> {

    Optional<Dish> findByIdAndRestaurantIdAndActiveTrue(Long id, Long restaurantId);

    Optional<Dish> findByIdAndRestaurantId(Long id, Long restaurantId);

    boolean existsByRestaurantIdAndCodeIgnoreCase(Long restaurantId, String code);

    boolean existsByRestaurantIdAndNameIgnoreCase(Long restaurantId, String name);

    List<Dish> findByRestaurantIdAndActiveTrueOrderByNameAsc(Long restaurantId);

    long countByRestaurantId(Long restaurantId);
}

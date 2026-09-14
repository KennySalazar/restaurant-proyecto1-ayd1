package com.restaurante.domain.repository;

import com.restaurante.domain.model.DishCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface DishCategoryRepository extends JpaRepository<DishCategory, Long> {

    Optional<DishCategory> findByIdAndRestaurantId(Long id, Long restaurantId);

    List<DishCategory> findByRestaurantIdOrderByVisualOrderAsc(Long restaurantId);
}

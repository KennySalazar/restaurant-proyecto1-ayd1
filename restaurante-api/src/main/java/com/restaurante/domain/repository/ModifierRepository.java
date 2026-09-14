package com.restaurante.domain.repository;

import com.restaurante.domain.model.Modifier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ModifierRepository extends JpaRepository<Modifier, Long> {

    Optional<Modifier> findByIdAndRestaurantIdAndActiveTrue(Long id, Long restaurantId);

    Optional<Modifier> findByIdAndRestaurantId(Long id, Long restaurantId);
}

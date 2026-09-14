package com.restaurante.domain.repository;

import com.restaurante.domain.model.Modifier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio JPA para la entidad de modificadores de platillos.
 */
@Repository
public interface ModifierRepository extends JpaRepository<Modifier, Long> {

    Optional<Modifier> findByIdAndRestaurantIdAndActiveTrue(Long id, Long restaurantId);

    Optional<Modifier> findByIdAndRestaurantId(Long id, Long restaurantId);

    boolean existsByRestaurantIdAndNameIgnoreCase(Long restaurantId, String name);

    boolean existsByRestaurantIdAndNameIgnoreCaseAndIdNot(Long restaurantId, String name, Long id);

    boolean existsByRestaurantIdAndCodeIgnoreCase(Long restaurantId, String code);

    boolean existsByRestaurantIdAndCodeIgnoreCaseAndIdNot(Long restaurantId, String code, Long id);

    long countByRestaurantId(Long restaurantId);

    @Query("SELECT m FROM Modifier m WHERE m.restaurantId = :restaurantId " +
           "AND (:search IS NULL OR LOWER(m.name) LIKE :search OR LOWER(m.code) LIKE :search) " +
           "AND (:active IS NULL OR m.active = :active) " +
           "ORDER BY m.name ASC")
    List<Modifier> findByRestaurantIdAndSearchAndActive(
            @Param("restaurantId") Long restaurantId,
            @Param("search") String search,
            @Param("active") Boolean active
    );

    @Query("SELECT DISTINCT m FROM Modifier m " +
           "JOIN DishModifier dm ON dm.modifier.id = m.id " +
           "WHERE m.restaurantId = :restaurantId AND dm.dish.id = :dishId " +
           "AND (:active IS NULL OR m.active = :active) " +
           "ORDER BY dm.visualOrder ASC, m.name ASC")
    List<Modifier> findByRestaurantIdAndDishIdAndActive(
            @Param("restaurantId") Long restaurantId,
            @Param("dishId") Long dishId,
            @Param("active") Boolean active
    );
}

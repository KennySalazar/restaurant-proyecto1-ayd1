package com.restaurante.domain.repository;

import com.restaurante.domain.model.Dish;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface DishRepository extends JpaRepository<Dish, Long> {

    @EntityGraph(attributePaths = {"category"})
    Optional<Dish> findByIdAndRestaurantIdAndActiveTrue(Long id, Long restaurantId);

    @EntityGraph(attributePaths = {"category"})
    Optional<Dish> findByIdAndRestaurantId(Long id, Long restaurantId);

    boolean existsByRestaurantIdAndCodeIgnoreCase(Long restaurantId, String code);

    boolean existsByRestaurantIdAndCodeIgnoreCaseAndIdNot(Long restaurantId, String code, Long id);

    boolean existsByRestaurantIdAndNameIgnoreCase(Long restaurantId, String name);

    boolean existsByRestaurantIdAndNameIgnoreCaseAndIdNot(Long restaurantId, String name, Long id);

    long countByRestaurantId(Long restaurantId);

    @Query(value = """
        SELECT c.nombre
        FROM restaurante.combos c
        JOIN restaurante.combo_detalles cd ON cd.combo_id = c.id
        WHERE cd.platillo_id = :dishId
          AND c.activo = true
        ORDER BY c.nombre ASC
        """, nativeQuery = true)
    List<String> findActiveComboNamesByDishId(@Param("dishId") Long dishId);

    @EntityGraph(attributePaths = {"category"})
    @Query("""
        SELECT d FROM Dish d
        WHERE d.restaurantId = :restaurantId
          AND d.active = true
        ORDER BY d.name ASC
        """)
    List<Dish> findByRestaurantIdAndActiveTrueOrderByNameAsc(@Param("restaurantId") Long restaurantId);

    @EntityGraph(attributePaths = {"category"})
    @Query("""
        SELECT d FROM Dish d
        WHERE d.restaurantId = :restaurantId
          AND (:active IS NULL OR d.active = :active)
        ORDER BY d.name ASC
        """)
    List<Dish> findByRestaurantIdOrderByNameAsc(
            @Param("restaurantId") Long restaurantId,
            @Param("active") Boolean active
    );

    @EntityGraph(attributePaths = {"category"})
    @Query("""
        SELECT d FROM Dish d
        WHERE d.restaurantId = :restaurantId
          AND (:active IS NULL OR d.active = :active)
          AND d.category.id = :categoryId
        ORDER BY d.name ASC
        """)
    List<Dish> findByRestaurantIdAndCategoryIdOrderByNameAsc(
            @Param("restaurantId") Long restaurantId,
            @Param("categoryId") Long categoryId,
            @Param("active") Boolean active
    );

    @EntityGraph(attributePaths = {"category"})
    @Query("""
        SELECT d FROM Dish d
        WHERE d.restaurantId = :restaurantId
          AND (:active IS NULL OR d.active = :active)
          AND (LOWER(d.name) LIKE :pattern OR LOWER(d.code) LIKE :pattern)
        ORDER BY d.name ASC
        """)
    List<Dish> findByRestaurantIdAndSearchPattern(
            @Param("restaurantId") Long restaurantId,
            @Param("pattern") String pattern,
            @Param("active") Boolean active
    );

    @EntityGraph(attributePaths = {"category"})
    @Query("""
        SELECT d FROM Dish d
        WHERE d.restaurantId = :restaurantId
          AND (:active IS NULL OR d.active = :active)
          AND d.category.id = :categoryId
          AND (LOWER(d.name) LIKE :pattern OR LOWER(d.code) LIKE :pattern)
        ORDER BY d.name ASC
        """)
    List<Dish> findByRestaurantIdAndCategoryIdAndSearchPattern(
            @Param("restaurantId") Long restaurantId,
            @Param("categoryId") Long categoryId,
            @Param("pattern") String pattern,
            @Param("active") Boolean active
    );
}

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
          AND d.active = true
          AND d.category.id = :categoryId
        ORDER BY d.name ASC
        """)
    List<Dish> findByRestaurantIdAndActiveTrueAndCategoryIdOrderByNameAsc(
            @Param("restaurantId") Long restaurantId,
            @Param("categoryId") Long categoryId
    );

    @EntityGraph(attributePaths = {"category"})
    @Query("""
        SELECT d FROM Dish d
        WHERE d.restaurantId = :restaurantId
          AND d.active = true
          AND (LOWER(d.name) LIKE :pattern OR LOWER(d.code) LIKE :pattern)
        ORDER BY d.name ASC
        """)
    List<Dish> findByRestaurantIdAndActiveTrueAndSearchPattern(
            @Param("restaurantId") Long restaurantId,
            @Param("pattern") String pattern
    );

    @EntityGraph(attributePaths = {"category"})
    @Query("""
        SELECT d FROM Dish d
        WHERE d.restaurantId = :restaurantId
          AND d.active = true
          AND d.category.id = :categoryId
          AND (LOWER(d.name) LIKE :pattern OR LOWER(d.code) LIKE :pattern)
        ORDER BY d.name ASC
        """)
    List<Dish> findByRestaurantIdAndActiveTrueAndCategoryIdAndSearchPattern(
            @Param("restaurantId") Long restaurantId,
            @Param("categoryId") Long categoryId,
            @Param("pattern") String pattern
    );
}

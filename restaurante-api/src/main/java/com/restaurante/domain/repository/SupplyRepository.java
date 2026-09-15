package com.restaurante.domain.repository;

import com.restaurante.domain.model.Supply;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SupplyRepository extends JpaRepository<Supply, Long> {

  boolean existsByRestaurantIdAndNameIgnoreCase(Long restaurantId, String name);

  boolean existsByRestaurantIdAndNameIgnoreCaseAndIdNot(Long restaurantId, String name, Long id);

  boolean existsByRestaurantIdAndCodeIgnoreCase(Long restaurantId, String code);

  boolean existsByRestaurantIdAndCodeIgnoreCaseAndIdNot(Long restaurantId, String code, Long id);

  boolean existsByIdAndRestaurantId(Long id, Long restaurantId);

  long countByRestaurantId(Long restaurantId);

  @EntityGraph(attributePaths = { "category", "measurementUnit" })
  List<Supply> findByRestaurantIdAndActiveTrueOrderByCreatedAtDesc(Long restaurantId);

  @EntityGraph(attributePaths = { "category", "measurementUnit" })
  Optional<Supply> findByIdAndRestaurantId(Long id, Long restaurantId);

  @EntityGraph(attributePaths = { "category", "measurementUnit" })
  Optional<Supply> findByIdAndRestaurantIdAndActiveTrue(Long id, Long restaurantId);

  @EntityGraph(attributePaths = { "category", "measurementUnit" })
  @org.springframework.data.jpa.repository.Query("""
      SELECT s FROM Supply s
      WHERE s.restaurantId = :restaurantId
        AND s.active = true
      ORDER BY s.name ASC
      """)
  List<Supply> findByRestaurantIdAndActiveTrueOrderByNameAsc(Long restaurantId);

  @EntityGraph(attributePaths = { "category", "measurementUnit" })
  @org.springframework.data.jpa.repository.Query("""
      SELECT s FROM Supply s
      WHERE s.restaurantId = :restaurantId
        AND s.active = true
        AND s.category.id = :categoryId
      ORDER BY s.name ASC
      """)
  List<Supply> findByRestaurantIdAndActiveTrueAndCategoryIdOrderByNameAsc(Long restaurantId, Long categoryId);

  @EntityGraph(attributePaths = { "category", "measurementUnit" })
  @org.springframework.data.jpa.repository.Query("""
      SELECT s FROM Supply s
      WHERE s.restaurantId = :restaurantId
        AND s.active = true
        AND (LOWER(s.name) LIKE :pattern OR LOWER(s.code) LIKE :pattern)
      ORDER BY s.name ASC
      """)
  List<Supply> findByRestaurantIdAndActiveTrueAndSearchPattern(Long restaurantId, String pattern);

  @EntityGraph(attributePaths = { "category", "measurementUnit" })
  @org.springframework.data.jpa.repository.Query("""
      SELECT s FROM Supply s
      WHERE s.restaurantId = :restaurantId
        AND s.active = true
        AND s.category.id = :categoryId
        AND (LOWER(s.name) LIKE :pattern OR LOWER(s.code) LIKE :pattern)
      ORDER BY s.name ASC
      """)
  List<Supply> findByRestaurantIdAndActiveTrueAndCategoryIdAndSearchPattern(Long restaurantId, Long categoryId, String pattern);

  @EntityGraph(attributePaths = { "category", "measurementUnit" })
  @org.springframework.data.jpa.repository.Query("""
      SELECT s FROM Supply s
      WHERE s.restaurantId = :restaurantId
        AND s.active = true
        AND s.currentStock <= s.minimumStock
      ORDER BY s.currentStock ASC, s.name ASC
      """)
  List<Supply> findLowStockSupplies(Long restaurantId);
}

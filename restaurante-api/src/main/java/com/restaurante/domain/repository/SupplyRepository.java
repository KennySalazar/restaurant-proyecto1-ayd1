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
        AND (:categoryId IS NULL OR s.category.id = :categoryId)
        AND (:search IS NULL OR LOWER(s.name) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(s.code) LIKE LOWER(CONCAT('%', :search, '%')))
      ORDER BY s.name ASC
      """)
  List<Supply> searchCatalog(Long restaurantId, Long categoryId, String search);
}

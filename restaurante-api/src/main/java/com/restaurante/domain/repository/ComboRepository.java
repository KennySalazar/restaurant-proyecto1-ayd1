package com.restaurante.domain.repository;

import com.restaurante.domain.model.Combo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio JPA para la entidad de combos y promociones.
 */
@Repository
public interface ComboRepository extends JpaRepository<Combo, Long> {

    Optional<Combo> findByIdAndRestaurantId(Long id, Long restaurantId);

    Optional<Combo> findByIdAndRestaurantIdAndActiveTrue(Long id, Long restaurantId);

    boolean existsByRestaurantIdAndNameIgnoreCase(Long restaurantId, String name);

    boolean existsByRestaurantIdAndNameIgnoreCaseAndIdNot(Long restaurantId, String name, Long id);

    boolean existsByRestaurantIdAndCodeIgnoreCase(Long restaurantId, String code);

    boolean existsByRestaurantIdAndCodeIgnoreCaseAndIdNot(Long restaurantId, String code, Long id);

    long countByRestaurantId(Long restaurantId);

    @Query("SELECT DISTINCT c FROM Combo c " +
           "LEFT JOIN FETCH c.details cd " +
           "LEFT JOIN FETCH cd.dish d " +
           "WHERE c.restaurantId = :restaurantId " +
           "AND (:active IS NULL OR c.active = :active) " +
           "AND (:search IS NULL OR LOWER(c.name) LIKE :search OR LOWER(c.code) LIKE :search) " +
           "ORDER BY c.name ASC")
    List<Combo> findByRestaurantIdAndFilters(
            @Param("restaurantId") Long restaurantId,
            @Param("active") Boolean active,
            @Param("search") String search
    );

    @Query("SELECT DISTINCT c FROM Combo c " +
           "LEFT JOIN FETCH c.details cd " +
           "LEFT JOIN FETCH cd.dish d " +
           "WHERE c.id = :id AND c.restaurantId = :restaurantId")
    Optional<Combo> findByIdWithDetailsAndDish(
            @Param("id") Long id,
            @Param("restaurantId") Long restaurantId
    );
}

package com.restaurante.domain.repository;

import com.restaurante.domain.model.RecipeVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface RecipeVersionRepository extends JpaRepository<RecipeVersion, Long> {

    @Query("SELECT rv FROM RecipeVersion rv WHERE rv.dish.id = :dishId AND rv.status = 'VIGENTE'")
    Optional<RecipeVersion> findActiveByDishId(@Param("dishId") Long dishId);

    @Query("SELECT DISTINCT rv FROM RecipeVersion rv " +
           "LEFT JOIN FETCH rv.details d " +
           "LEFT JOIN FETCH d.supply s " +
           "LEFT JOIN FETCH d.measurementUnit mu " +
           "LEFT JOIN FETCH s.measurementUnit smu " +
           "WHERE rv.dish.id = :dishId AND rv.status = 'VIGENTE'")
    Optional<RecipeVersion> findActiveWithDetailsByDishId(@Param("dishId") Long dishId);

    @Query("SELECT DISTINCT rv FROM RecipeVersion rv " +
           "LEFT JOIN FETCH rv.details d " +
           "LEFT JOIN FETCH d.supply s " +
           "LEFT JOIN FETCH d.measurementUnit mu " +
           "LEFT JOIN FETCH s.measurementUnit smu " +
           "WHERE rv.dish.restaurantId = :restaurantId AND rv.status = 'VIGENTE'")
    List<RecipeVersion> findActiveWithDetailsByRestaurantId(@Param("restaurantId") Long restaurantId);

    @Query("SELECT COUNT(rv) > 0 FROM RecipeVersion rv WHERE rv.dish.id = :dishId AND rv.status = 'VIGENTE'")
    boolean existsActiveByDishId(@Param("dishId") Long dishId);

    @Query("SELECT COALESCE(MAX(rv.versionNumber), 0) FROM RecipeVersion rv WHERE rv.dish.id = :dishId")
    int findMaxVersionNumberByDishId(@Param("dishId") Long dishId);

    List<RecipeVersion> findByDishIdOrderByVersionNumberDesc(Long dishId);

    List<RecipeVersion> findByDishIdOrderByVersionNumberAsc(Long dishId);

    Optional<RecipeVersion> findByDishIdAndVersionNumber(Long dishId, Integer versionNumber);
}

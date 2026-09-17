package com.restaurante.domain.repository;

import com.restaurante.domain.model.DishModifier;
import com.restaurante.domain.model.DishModifierId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

/**
 * Repositorio JPA para la asociación entre platillos y modificadores.
 */
@Repository
public interface DishModifierRepository extends JpaRepository<DishModifier, DishModifierId> {

    @Query("SELECT dm FROM DishModifier dm JOIN FETCH dm.dish d JOIN FETCH d.category WHERE dm.modifier.id = :modifierId ORDER BY d.name ASC")
    List<DishModifier> findByModifierIdWithDish(@Param("modifierId") Long modifierId);

    @Query("SELECT dm FROM DishModifier dm JOIN FETCH dm.modifier m WHERE dm.dish.id = :dishId ORDER BY dm.visualOrder ASC, m.name ASC")
    List<DishModifier> findByDishIdWithModifier(@Param("dishId") Long dishId);

    @Query("SELECT dm FROM DishModifier dm JOIN FETCH dm.modifier m WHERE dm.dish.id IN :dishIds ORDER BY dm.dish.id ASC, dm.visualOrder ASC, m.name ASC")
    List<DishModifier> findByDishIdInWithModifier(@Param("dishIds") Collection<Long> dishIds);

    @Modifying
    @Query("DELETE FROM DishModifier dm WHERE dm.modifier.id = :modifierId")
    void deleteByModifierId(@Param("modifierId") Long modifierId);

    @Modifying
    @Query("DELETE FROM DishModifier dm WHERE dm.dish.id = :dishId")
    void deleteByDishId(@Param("dishId") Long dishId);

    boolean existsByIdDishIdAndIdModifierId(Long dishId, Long modifierId);
}

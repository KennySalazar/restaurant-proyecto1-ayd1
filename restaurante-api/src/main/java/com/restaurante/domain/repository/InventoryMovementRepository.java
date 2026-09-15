package com.restaurante.domain.repository;

import com.restaurante.domain.model.InventoryMovement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio JPA para los movimientos de kardex en inventario.
 */
@Repository
public interface InventoryMovementRepository extends JpaRepository<InventoryMovement, Long> {

    @Query("SELECT mi FROM InventoryMovement mi JOIN FETCH mi.supply s JOIN FETCH s.measurementUnit WHERE mi.comandaDetail.comanda.id = :comandaId ORDER BY mi.id ASC")
    List<InventoryMovement> findByComandaIdWithSupply(@Param("comandaId") Long comandaId);

    @Query("SELECT COUNT(mi) > 0 FROM InventoryMovement mi WHERE mi.comandaDetail.comanda.id = :comandaId AND mi.type = :type")
    boolean existsByComandaIdAndType(@Param("comandaId") Long comandaId, @Param("type") String type);

    @Query("SELECT mi FROM InventoryMovement mi WHERE mi.supply.id = :supplyId ORDER BY mi.createdAt DESC")
    List<InventoryMovement> findBySupplyIdOrderByCreatedAtDesc(@Param("supplyId") Long supplyId);
}

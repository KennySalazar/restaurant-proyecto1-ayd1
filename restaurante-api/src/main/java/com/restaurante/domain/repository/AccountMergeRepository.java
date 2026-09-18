package com.restaurante.domain.repository;

import com.restaurante.domain.model.AccountMerge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio JPA para auditoría y registro de fusiones de cuentas de mesa.
 */
@Repository
public interface AccountMergeRepository extends JpaRepository<AccountMerge, Long> {

    boolean existsByOriginAccountId(Long originAccountId);

    Optional<AccountMerge> findByOriginAccountId(Long originAccountId);

    @Query("SELECT m FROM AccountMerge m WHERE EXISTS ("
            + "SELECT 1 FROM Account a WHERE a.id = m.destinationAccountId "
            + "AND a.restaurantId = :restaurantId "
            + "AND a.status IN ('ABIERTA', 'LISTA_COBRO', 'PARCIALMENTE_PAGADA'))")
    List<AccountMerge> findActiveFusionsByRestaurant(@Param("restaurantId") Long restaurantId);
}

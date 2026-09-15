package com.restaurante.domain.repository;

import com.restaurante.domain.model.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio JPA para la entidad Account (cuentas).
 */
@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

    Optional<Account> findByIdAndRestaurantId(Long id, Long restaurantId);

    @Query("SELECT a FROM Account a WHERE a.tableId = :tableId AND a.restaurantId = :restaurantId AND a.status IN ('ABIERTA', 'LISTA_COBRO', 'PARCIALMENTE_PAGADA')")
    Optional<Account> findActiveByTableIdAndRestaurantId(@Param("tableId") Long tableId, @Param("restaurantId") Long restaurantId);

    boolean existsByRestaurantIdAndAccountNumber(Long restaurantId, String accountNumber);
}

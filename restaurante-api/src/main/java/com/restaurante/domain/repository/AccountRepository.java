package com.restaurante.domain.repository;

import com.restaurante.domain.model.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Repositorio JPA para la entidad Account (cuentas).
 */
@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

    Optional<Account> findByIdAndRestaurantId(Long id, Long restaurantId);

    @Query("SELECT a FROM Account a WHERE a.tableId = :tableId AND a.restaurantId = :restaurantId AND a.status IN ('ABIERTA', 'LISTA_COBRO', 'PARCIALMENTE_PAGADA')")
    Optional<Account> findActiveByTableIdAndRestaurantId(@Param("tableId") Long tableId, @Param("restaurantId") Long restaurantId);

    @Query("SELECT a FROM Account a WHERE a.restaurantId = :restaurantId AND a.status IN ('ABIERTA', 'LISTA_COBRO', 'PARCIALMENTE_PAGADA') ORDER BY a.openedAt DESC")
    java.util.List<Account> findActiveByRestaurantId(@Param("restaurantId") Long restaurantId);

    @Query("""
        SELECT a
        FROM Account a
        WHERE a.restaurantId = :restaurantId
          AND a.status IN ('LISTA_COBRO', 'PARCIALMENTE_PAGADA')
        ORDER BY a.openedAt ASC
        """)
    java.util.List<Account> findReadyForPaymentByRestaurantId(
            @Param("restaurantId") Long restaurantId
    );

    @Query("SELECT a FROM Account a WHERE a.waiterId = :waiterId AND a.status IN ('ABIERTA', 'LISTA_COBRO', 'PARCIALMENTE_PAGADA') ORDER BY a.openedAt DESC")
    java.util.List<Account> findActiveByWaiterId(@Param("waiterId") Long waiterId);

    boolean existsByRestaurantIdAndAccountNumber(Long restaurantId, String accountNumber);

    @Query(value = """
    SELECT ROUND(
        COALESCE(
            SUM(
                cd.cantidad * (
                    cd.precio_unitario_snapshot
                    + COALESCE((
                        SELECT SUM(
                            cdm.cantidad * cdm.precio_adicional_snapshot
                        )
                        FROM restaurante.comanda_detalle_modificadores cdm
                        WHERE cdm.comanda_detalle_id = cd.id
                    ), 0)
                )
            ),
            0
        ),
        2
    )
    FROM restaurante.comandas co
    JOIN restaurante.comanda_detalles cd
      ON cd.comanda_id = co.id
    WHERE co.cuenta_id = :accountId
      AND cd.estado = 'ENTREGADO'
    """, nativeQuery = true)
    BigDecimal calculateDeliveredSubtotal(
            @Param("accountId") Long accountId
    );

    @Query(value = """
    SELECT s.subtotal_snapshot
    FROM restaurante.subcuentas s
    WHERE s.id = :subAccountId
      AND s.cuenta_id = :accountId
      AND s.estado = 'PENDIENTE'
    """, nativeQuery = true)
    Optional<BigDecimal> findPendingSubAccountSubtotal(
            @Param("accountId") Long accountId,
            @Param("subAccountId") Long subAccountId
    );
}

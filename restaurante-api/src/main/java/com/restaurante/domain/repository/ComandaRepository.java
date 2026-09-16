package com.restaurante.domain.repository;

import com.restaurante.domain.model.Comanda;
import com.restaurante.domain.model.ComandaStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio JPA para la entidad Comanda (comandas).
 */
@Repository
public interface ComandaRepository extends JpaRepository<Comanda, Long> {

    @Query("SELECT c FROM Comanda c WHERE c.id = :id AND c.account.restaurantId = :restaurantId")
    Optional<Comanda> findByIdAndRestaurantId(@Param("id") Long id, @Param("restaurantId") Long restaurantId);

    @Query("""
       SELECT DISTINCT c
       FROM Comanda c
       LEFT JOIN FETCH c.details d
       LEFT JOIN FETCH d.dish
       LEFT JOIN FETCH d.combo
       LEFT JOIN FETCH d.recipeVersion
       WHERE c.id = :id
         AND c.account.restaurantId = :restaurantId
       """)
    Optional<Comanda> findByIdWithDetailsAndRestaurantId(
            @Param("id") Long id,
            @Param("restaurantId") Long restaurantId
    );
    @Query("SELECT COALESCE(MAX(c.roundNumber), 0) FROM Comanda c WHERE c.account.id = :accountId")
    short findMaxRoundNumberByAccountId(@Param("accountId") Long accountId);

    @Query("SELECT c FROM Comanda c WHERE c.account.id = :accountId ORDER BY c.roundNumber ASC")
    List<Comanda> findByAccountId(@Param("accountId") Long accountId);

    @Query("""
           SELECT c
           FROM Comanda c
           WHERE c.account.id = :accountId
             AND c.status = com.restaurante.domain.model.ComandaStatus.BORRADOR
           ORDER BY c.roundNumber DESC
           """)
    List<Comanda> findDraftComandasByAccountId(@Param("accountId") Long accountId);

    @Query("""
           SELECT DISTINCT c
           FROM Comanda c
           JOIN FETCH c.account a
           LEFT JOIN FETCH c.waiterUser w
           LEFT JOIN FETCH c.details d
           LEFT JOIN FETCH d.dish
           LEFT JOIN FETCH d.combo
           WHERE c.account.restaurantId = :restaurantId
             AND c.status IN (:statuses)
           ORDER BY c.sentAt ASC, c.id ASC
           """)
    List<Comanda> findKitchenComandasByStatusIn(
            @Param("restaurantId") Long restaurantId,
            @Param("statuses") List<ComandaStatus> statuses
    );
}

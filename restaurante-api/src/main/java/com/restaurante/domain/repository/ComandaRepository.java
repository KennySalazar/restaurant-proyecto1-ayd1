package com.restaurante.domain.repository;

import com.restaurante.domain.model.Comanda;
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
}

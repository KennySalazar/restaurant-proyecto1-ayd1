package com.restaurante.domain.repository;

import com.restaurante.domain.model.ComandaDetail;
import com.restaurante.domain.model.ComandaDetailStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ComandaDetailRepository extends JpaRepository<ComandaDetail, Long> {

    @Query("""
           SELECT DISTINCT d
           FROM ComandaDetail d
           JOIN FETCH d.comanda c
           LEFT JOIN FETCH c.account a
           LEFT JOIN FETCH d.modifiers m
           LEFT JOIN FETCH m.modifier
           WHERE d.id = :id
           """)
    Optional<ComandaDetail> findByIdWithComandaAndModifiers(
            @Param("id") Long id
    );

    @Query("""
           SELECT DISTINCT d
           FROM ComandaDetail d
           JOIN FETCH d.comanda c
           LEFT JOIN FETCH c.account a
           LEFT JOIN FETCH c.waiterUser w
           LEFT JOIN FETCH d.dish
           LEFT JOIN FETCH d.combo
           LEFT JOIN FETCH d.modifiers m
           LEFT JOIN FETCH m.modifier
           WHERE d.id = :id
           """)
    Optional<ComandaDetail> findByIdWithAllDetails(
            @Param("id") Long id
    );

    @Query("""
           SELECT DISTINCT d
           FROM ComandaDetail d
           LEFT JOIN FETCH d.modifiers m
           LEFT JOIN FETCH m.modifier
           WHERE d.comanda.id = :comandaId
           """)
    List<ComandaDetail> findByComandaIdWithModifiers(
            @Param("comandaId") Long comandaId
    );

    @Query("""
           SELECT DISTINCT d
           FROM ComandaDetail d
           LEFT JOIN FETCH d.modifiers m
           LEFT JOIN FETCH m.modifier
           WHERE d.comanda.id IN (:comandaIds)
           """)
    List<ComandaDetail> findByComandaIdInWithModifiers(
            @Param("comandaIds") List<Long> comandaIds
    );

    @Query("""
           SELECT DISTINCT d
           FROM ComandaDetail d
           JOIN FETCH d.comanda c
           JOIN FETCH c.account a
           LEFT JOIN FETCH c.waiterUser w
           LEFT JOIN FETCH d.dish
           LEFT JOIN FETCH d.combo
           LEFT JOIN FETCH d.modifiers m
           LEFT JOIN FETCH m.modifier
           WHERE a.restaurantId = :restaurantId
             AND (:comandaId IS NULL OR c.id = :comandaId)
             AND (:cuentaId IS NULL OR a.id = :cuentaId)
             AND (:mesaId IS NULL OR a.tableId = :mesaId)
             AND (:meseroId IS NULL OR c.waiterId = :meseroId)
             AND (:status IS NULL OR d.status = :status)
           ORDER BY c.roundNumber ASC, d.id ASC
           """)
    List<ComandaDetail> findDishProgress(
            @Param("restaurantId") Long restaurantId,
            @Param("comandaId") Long comandaId,
            @Param("cuentaId") Long cuentaId,
            @Param("mesaId") Long mesaId,
            @Param("meseroId") Long meseroId,
            @Param("status") ComandaDetailStatus status
    );

    @Query("""
           SELECT DISTINCT d
           FROM ComandaDetail d
           JOIN FETCH d.comanda c
           JOIN FETCH c.account a
           LEFT JOIN FETCH c.waiterUser w
           LEFT JOIN FETCH d.dish
           LEFT JOIN FETCH d.combo
           LEFT JOIN FETCH d.modifiers m
           LEFT JOIN FETCH m.modifier
           WHERE a.restaurantId = :restaurantId
             AND d.status IN (com.restaurante.domain.model.ComandaDetailStatus.RECIBIDO, com.restaurante.domain.model.ComandaDetailStatus.EN_PREPARACION)
           ORDER BY d.receivedAt ASC, d.id ASC
           """)
    List<ComandaDetail> findActiveDishesInKitchen(
            @Param("restaurantId") Long restaurantId
    );

    @Query("""
           SELECT COUNT(d)
           FROM ComandaDetail d
           WHERE d.comanda.account.id = :accountId
              AND d.status NOT IN (com.restaurante.domain.model.ComandaDetailStatus.CANCELADO, com.restaurante.domain.model.ComandaDetailStatus.NO_DISPONIBLE)
           """)
    long countActiveByAccountId(@Param("accountId") Long accountId);

    long countByComandaId(Long comandaId);
}
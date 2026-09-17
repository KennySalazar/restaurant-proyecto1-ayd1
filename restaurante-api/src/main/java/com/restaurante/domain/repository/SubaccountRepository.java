package com.restaurante.domain.repository;

import com.restaurante.domain.model.Subaccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio JPA para la entidad Subaccount (subcuentas).
 */
@Repository
public interface SubaccountRepository extends JpaRepository<Subaccount, Long> {

    List<Subaccount> findByAccountIdOrderBySubaccountNumberAsc(Long accountId);

    List<Subaccount> findByAccountIdAndStatusNot(Long accountId, String status);

    @Query("SELECT s FROM Subaccount s LEFT JOIN FETCH s.details d LEFT JOIN FETCH d.comandaDetail cd WHERE s.id = :id")
    Optional<Subaccount> findByIdWithDetails(@Param("id") Long id);

    @Query("SELECT DISTINCT s FROM Subaccount s LEFT JOIN FETCH s.details d LEFT JOIN FETCH d.comandaDetail cd WHERE s.account.id = :accountId ORDER BY s.subaccountNumber ASC")
    List<Subaccount> findByAccountIdWithDetails(@Param("accountId") Long accountId);

    void deleteByAccountIdAndStatus(Long accountId, String status);

    boolean existsByAccountIdAndStatus(Long accountId, String status);

    boolean existsByAccountIdAndStatusNot(Long accountId, String status);
}

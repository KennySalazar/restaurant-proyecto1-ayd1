package com.restaurante.domain.repository;

import com.restaurante.domain.model.SubaccountDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio JPA para la entidad SubaccountDetail (subcuenta_detalles).
 */
@Repository
public interface SubaccountDetailRepository extends JpaRepository<SubaccountDetail, Long> {

    List<SubaccountDetail> findBySubaccountId(Long subaccountId);

    Optional<SubaccountDetail> findBySubaccountIdAndComandaDetailId(Long subaccountId, Long comandaDetailId);

    @Query("""
        SELECT sd FROM SubaccountDetail sd
        JOIN sd.subaccount s
        WHERE s.account.id = :accountId
          AND s.status <> 'CANCELADA'
          AND sd.comandaDetail.id = :comandaDetailId
    """)
    List<SubaccountDetail> findActiveByAccountIdAndComandaDetailId(
            @Param("accountId") Long accountId,
            @Param("comandaDetailId") Long comandaDetailId
    );

    @Query("""
        SELECT sd FROM SubaccountDetail sd
        JOIN sd.subaccount s
        WHERE s.account.id = :accountId
          AND s.status <> 'CANCELADA'
    """)
    List<SubaccountDetail> findActiveByAccountId(@Param("accountId") Long accountId);
}

package com.restaurante.domain.repository;

import com.restaurante.domain.model.AccountTransfer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio JPA para auditorías de transferencias de cuentas entre mesas.
 */
@Repository
public interface AccountTransferRepository extends JpaRepository<AccountTransfer, Long> {

    List<AccountTransfer> findAllByAccountIdOrderByPerformedAtDesc(Long accountId);
}

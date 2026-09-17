package com.restaurante.domain.repository;

import com.restaurante.domain.model.AccountMerge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio JPA para auditoría y registro de fusiones de cuentas de mesa.
 */
@Repository
public interface AccountMergeRepository extends JpaRepository<AccountMerge, Long> {

    boolean existsByOriginAccountId(Long originAccountId);

    Optional<AccountMerge> findByOriginAccountId(Long originAccountId);
}

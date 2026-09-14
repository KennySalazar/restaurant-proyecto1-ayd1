package com.restaurante.domain.repository;

import com.restaurante.domain.model.CashTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CashTransactionRepository
        extends JpaRepository<CashTransaction, Long> {
}
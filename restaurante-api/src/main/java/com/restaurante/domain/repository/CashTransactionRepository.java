package com.restaurante.domain.repository;

import com.restaurante.domain.model.CashTransaction;
import com.restaurante.domain.model.CashTransactionType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CashTransactionRepository
        extends JpaRepository<CashTransaction, Long> {

    boolean existsByPaymentId(Long paymentId);

    boolean existsByInvoiceIdAndType(
            Long invoiceId,
            CashTransactionType type
    );
}
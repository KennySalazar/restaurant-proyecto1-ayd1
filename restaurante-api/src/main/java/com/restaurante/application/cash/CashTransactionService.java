package com.restaurante.application.cash;

import com.restaurante.domain.model.CashTransaction;
import com.restaurante.domain.model.CashTransactionType;
import com.restaurante.domain.repository.CashTransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class CashTransactionService {

    private final CashTransactionRepository cashTransactions;

    public CashTransactionService(
            CashTransactionRepository cashTransactions) {
        this.cashTransactions = cashTransactions;
    }

    @Transactional
    public void registerSale(
            Long shiftId,
            Long invoiceId,
            Long paymentId,
            Long cashierId,
            BigDecimal amount) {

        if (cashTransactions.existsByPaymentId(paymentId)) {
            return;
        }

        CashTransaction transaction = CashTransaction.sale(
                shiftId,
                invoiceId,
                paymentId,
                cashierId,
                amount
        );

        cashTransactions.save(transaction);
    }

    @Transactional
    public void registerTip(
            Long shiftId,
            Long invoiceId,
            Long cashierId,
            BigDecimal amount) {

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        if (cashTransactions.existsByInvoiceIdAndType(
                invoiceId,
                CashTransactionType.PROPINA)) {
            return;
        }

        CashTransaction transaction = CashTransaction.tip(
                shiftId,
                invoiceId,
                cashierId,
                amount
        );

        cashTransactions.save(transaction);
    }

    @Transactional
    public void registerPointsRedemption(
            Long shiftId,
            Long invoiceId,
            Long cashierId,
            BigDecimal amount,
            Long points) {

        if (points == null || points <= 0) {
            return;
        }

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        if (cashTransactions.existsByInvoiceIdAndType(
                invoiceId,
                CashTransactionType.REDENCION_PUNTOS)) {
            return;
        }

        CashTransaction transaction = CashTransaction.pointsRedemption(
                shiftId,
                invoiceId,
                cashierId,
                amount,
                points
        );

        cashTransactions.save(transaction);
    }
}
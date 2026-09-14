package com.restaurante.domain.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "transacciones_caja", schema = "restaurante")
public class CashTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "turno_caja_id", nullable = false)
    private Long cashShiftId;

    @Column(name = "factura_id")
    private Long invoiceId;

    @Column(name = "pago_id")
    private Long paymentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 25)
    private CashTransactionType type;

    @Column(name = "monto", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "puntos", nullable = false)
    private Long points;

    @Column(name = "descripcion", nullable = false, length = 500)
    private String description;

    @Column(name = "registrada_por_id", nullable = false)
    private Long registeredById;

    @Column(name = "registrada_en", nullable = false)
    private Instant registeredAt;

    protected CashTransaction() {
    }

    public static CashTransaction opening(
            Long shiftId,
            Long cashierId,
            BigDecimal amount) {

        CashTransaction transaction = new CashTransaction();
        transaction.cashShiftId = shiftId;
        transaction.type = CashTransactionType.APERTURA;
        transaction.amount = amount;
        transaction.points = 0L;
        transaction.description = "Apertura de turno de caja";
        transaction.registeredById = cashierId;
        transaction.registeredAt = Instant.now();

        return transaction;
    }
}
package com.restaurante.domain.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "turnos_caja", schema = "restaurante")
public class CashShift {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "caja_id", nullable = false)
    private Long cashRegisterId;

    @Column(name = "cajero_id", nullable = false)
    private Long cashierId;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 10)
    private CashShiftStatus status;

    @Column(name = "monto_inicial_efectivo", nullable = false, precision = 12, scale = 2)
    private BigDecimal initialCashAmount;

    @Column(name = "abierta_en", nullable = false)
    private Instant openedAt;

    @Column(name = "efectivo_esperado_cierre", precision = 12, scale = 2)
    private BigDecimal expectedCashAtClose;

    @Column(name = "efectivo_real_cierre", precision = 12, scale = 2)
    private BigDecimal actualCashAtClose;

    @Column(name = "diferencia_cierre", precision = 12, scale = 2)
    private BigDecimal closingDifference;

    @Column(name = "observaciones_apertura", length = 500)
    private String openingNotes;

    @Column(name = "observaciones_cierre", length = 500)
    private String closingNotes;

    @Column(name = "cerrada_en")
    private Instant closedAt;

    protected CashShift() {
    }

    public CashShift(
            Long cashRegisterId,
            Long cashierId,
            BigDecimal initialCashAmount,
            String openingNotes) {

        this.cashRegisterId = cashRegisterId;
        this.cashierId = cashierId;
        this.initialCashAmount = initialCashAmount;
        this.openingNotes = openingNotes;
        this.status = CashShiftStatus.ABIERTA;
        this.openedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Long getCashRegisterId() {
        return cashRegisterId;
    }

    public Long getCashierId() {
        return cashierId;
    }

    public CashShiftStatus getStatus() {
        return status;
    }

    public BigDecimal getInitialCashAmount() {
        return initialCashAmount;
    }

    public Instant getOpenedAt() {
        return openedAt;
    }

    public String getOpeningNotes() {
        return openingNotes;
    }
}
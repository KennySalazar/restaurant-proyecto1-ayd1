package com.restaurante.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Entidad que representa la auditoría de transferencia de una cuenta entre mesas del restaurante.
 */
@Entity
@Table(name = "transferencias_cuenta", schema = "restaurante")
public class AccountTransfer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cuenta_id", nullable = false)
    private Long accountId;

    @Column(name = "mesa_origen_id", nullable = false)
    private Long originTableId;

    @Column(name = "mesa_destino_id", nullable = false)
    private Long destinationTableId;

    @Column(name = "realizada_por_id", nullable = false)
    private Long performedById;

    @Column(name = "motivo", length = 500)
    private String reason;

    @Column(name = "realizada_en", nullable = false)
    private Instant performedAt;

    protected AccountTransfer() {
    }

    public AccountTransfer(Long accountId, Long originTableId, Long destinationTableId, Long performedById, String reason) {
        this.accountId = accountId;
        this.originTableId = originTableId;
        this.destinationTableId = destinationTableId;
        this.performedById = performedById;
        this.reason = reason;
        this.performedAt = Instant.now();
    }

    @PrePersist
    void onCreate() {
        if (this.performedAt == null) {
            this.performedAt = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public Long getAccountId() {
        return accountId;
    }

    public Long getOriginTableId() {
        return originTableId;
    }

    public Long getDestinationTableId() {
        return destinationTableId;
    }

    public Long getPerformedById() {
        return performedById;
    }

    public String getReason() {
        return reason;
    }

    public Instant getPerformedAt() {
        return performedAt;
    }
}

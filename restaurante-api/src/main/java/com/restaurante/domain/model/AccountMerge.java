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
 * Entidad que representa el registro histórico y de auditoría de fusión de cuentas de mesas en el restaurante.
 */
@Entity
@Table(name = "fusiones_cuenta", schema = "restaurante")
public class AccountMerge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cuenta_origen_id", nullable = false)
    private Long originAccountId;

    @Column(name = "cuenta_destino_id", nullable = false)
    private Long destinationAccountId;

    @Column(name = "realizada_por_id", nullable = false)
    private Long performedById;

    @Column(name = "motivo", length = 500)
    private String reason;

    @Column(name = "realizada_en", nullable = false)
    private Instant performedAt;

    protected AccountMerge() {
    }

    public AccountMerge(Long originAccountId, Long destinationAccountId, Long performedById, String reason) {
        this.originAccountId = originAccountId;
        this.destinationAccountId = destinationAccountId;
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

    public Long getOriginAccountId() {
        return originAccountId;
    }

    public Long getDestinationAccountId() {
        return destinationAccountId;
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

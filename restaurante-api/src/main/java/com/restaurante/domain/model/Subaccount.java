package com.restaurante.domain.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidad que representa una sub-cuenta derivada de una cuenta principal dividida por personas o por ítems.
 */
@Entity
@Table(name = "subcuentas", schema = "restaurante")
public class Subaccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cuenta_id", nullable = false)
    private Account account;

    @Column(name = "numero_subcuenta", nullable = false)
    private short subaccountNumber = 1;

    @Column(name = "nombre", length = 100)
    private String name;

    @Column(name = "tipo_division", nullable = false, length = 15)
    private String splitType; // "PERSONAS" o "ITEMS"

    @Column(name = "porcentaje_asignado", precision = 7, scale = 4)
    private BigDecimal assignedPercentage;

    @Column(name = "estado", nullable = false, length = 15)
    private String status = "PENDIENTE";

    @Column(name = "subtotal_snapshot", nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotalSnapshot = BigDecimal.ZERO;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "actualizado_en", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "subaccount", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SubaccountDetail> details = new ArrayList<>();

    protected Subaccount() {
    }

    public Subaccount(Account account, short subaccountNumber, String name, String splitType, BigDecimal assignedPercentage) {
        this.account = account;
        this.subaccountNumber = subaccountNumber > 0 ? subaccountNumber : 1;
        this.name = name;
        this.splitType = splitType;
        this.assignedPercentage = assignedPercentage;
        this.status = "PENDIENTE";
        this.subtotalSnapshot = BigDecimal.ZERO;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
        if (status == null) {
            status = "PENDIENTE";
        }
        if (subtotalSnapshot == null) {
            subtotalSnapshot = BigDecimal.ZERO;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }

    public short getSubaccountNumber() {
        return subaccountNumber;
    }

    public void setSubaccountNumber(short subaccountNumber) {
        this.subaccountNumber = subaccountNumber;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSplitType() {
        return splitType;
    }

    public void setSplitType(String splitType) {
        this.splitType = splitType;
    }

    public BigDecimal getAssignedPercentage() {
        return assignedPercentage;
    }

    public void setAssignedPercentage(BigDecimal assignedPercentage) {
        this.assignedPercentage = assignedPercentage;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public BigDecimal getSubtotalSnapshot() {
        return subtotalSnapshot;
    }

    public void setSubtotalSnapshot(BigDecimal subtotalSnapshot) {
        this.subtotalSnapshot = subtotalSnapshot != null ? subtotalSnapshot : BigDecimal.ZERO;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public List<SubaccountDetail> getDetails() {
        return details;
    }

    public void setDetails(List<SubaccountDetail> details) {
        this.details = details != null ? details : new ArrayList<>();
    }
}

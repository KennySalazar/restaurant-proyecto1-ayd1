package com.restaurante.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/**
 * Entidad que representa la asignación de un ítem o platillo de comanda a una sub-cuenta.
 */
@Entity
@Table(name = "subcuenta_detalles", schema = "restaurante")
public class SubaccountDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subcuenta_id", nullable = false)
    private Subaccount subaccount;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "comanda_detalle_id", nullable = false)
    private ComandaDetail comandaDetail;

    @Column(name = "cantidad_asignada", nullable = false, precision = 12, scale = 6)
    private BigDecimal assignedQuantity;

    protected SubaccountDetail() {
    }

    public SubaccountDetail(Subaccount subaccount, ComandaDetail comandaDetail, BigDecimal assignedQuantity) {
        this.subaccount = subaccount;
        this.comandaDetail = comandaDetail;
        this.assignedQuantity = assignedQuantity;
    }

    public Long getId() {
        return id;
    }

    public Subaccount getSubaccount() {
        return subaccount;
    }

    public void setSubaccount(Subaccount subaccount) {
        this.subaccount = subaccount;
    }

    public ComandaDetail getComandaDetail() {
        return comandaDetail;
    }

    public void setComandaDetail(ComandaDetail comandaDetail) {
        this.comandaDetail = comandaDetail;
    }

    public BigDecimal getAssignedQuantity() {
        return assignedQuantity;
    }

    public void setAssignedQuantity(BigDecimal assignedQuantity) {
        this.assignedQuantity = assignedQuantity;
    }
}

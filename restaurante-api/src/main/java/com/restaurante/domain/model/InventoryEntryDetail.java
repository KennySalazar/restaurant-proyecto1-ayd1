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
import java.time.LocalDate;

/**
 * Entidad que representa el detalle de línea de una entrada de inventario
 */
@Entity
@Table(name = "entrada_inventario_detalles", schema = "restaurante")
public class InventoryEntryDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "entrada_id", nullable = false)
    private InventoryEntry entry;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "insumo_id", nullable = false)
    private Supply supply;

    @Column(name = "cantidad", nullable = false, precision = 18, scale = 4)
    private BigDecimal quantity;

    @Column(name = "costo_unitario", nullable = false, precision = 14, scale = 4)
    private BigDecimal unitCost;

    @Column(name = "costo_total", insertable = false, updatable = false, precision = 18, scale = 4)
    private BigDecimal totalCost;

    @Column(name = "lote", length = 80)
    private String batchNumber;

    @Column(name = "fecha_vencimiento")
    private LocalDate expirationDate;

    protected InventoryEntryDetail() {
    }

    public InventoryEntryDetail(InventoryEntry entry, Supply supply, BigDecimal quantity,
            BigDecimal unitCost, String batchNumber, LocalDate expirationDate) {
        this.entry = entry;
        this.supply = supply;
        this.quantity = quantity;
        this.unitCost = unitCost;
        this.batchNumber = batchNumber;
        this.expirationDate = expirationDate;
    }

    public Long getId() {
        return id;
    }

    public InventoryEntry getEntry() {
        return entry;
    }

    public void setEntry(InventoryEntry entry) {
        this.entry = entry;
    }

    public Supply getSupply() {
        return supply;
    }

    public void setSupply(Supply supply) {
        this.supply = supply;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getUnitCost() {
        return unitCost;
    }

    public void setUnitCost(BigDecimal unitCost) {
        this.unitCost = unitCost;
    }

    public BigDecimal getTotalCost() {
        return totalCost;
    }

    public String getBatchNumber() {
        return batchNumber;
    }

    public void setBatchNumber(String batchNumber) {
        this.batchNumber = batchNumber;
    }

    public LocalDate getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(LocalDate expirationDate) {
        this.expirationDate = expirationDate;
    }
}

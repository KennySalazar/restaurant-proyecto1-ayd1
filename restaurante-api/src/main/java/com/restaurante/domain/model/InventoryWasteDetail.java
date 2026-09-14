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
 * Entidad que representa el detalle de línea de una merma de inventario.
 */
@Entity
@Table(name = "merma_detalles", schema = "restaurante")
public class InventoryWasteDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "merma_id", nullable = false)
    private InventoryWaste waste;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "insumo_id", nullable = false)
    private Supply supply;

    @Column(name = "cantidad", nullable = false, precision = 18, scale = 4)
    private BigDecimal quantity;

    @Column(name = "costo_unitario_snapshot", nullable = false, precision = 14, scale = 4)
    private BigDecimal unitCostSnapshot;

    @Column(name = "costo_total", insertable = false, updatable = false, precision = 18, scale = 4)
    private BigDecimal totalCost;

    @Column(name = "lote", length = 80)
    private String batchNumber;

    @Column(name = "observacion", length = 255)
    private String notes;

    protected InventoryWasteDetail() {
    }

    public InventoryWasteDetail(InventoryWaste waste, Supply supply, BigDecimal quantity,
            BigDecimal unitCostSnapshot, String batchNumber, String notes) {
        this.waste = waste;
        this.supply = supply;
        this.quantity = quantity;
        this.unitCostSnapshot = unitCostSnapshot;
        this.batchNumber = batchNumber;
        this.notes = notes;
    }

    public Long getId() {
        return id;
    }

    public InventoryWaste getWaste() {
        return waste;
    }

    public void setWaste(InventoryWaste waste) {
        this.waste = waste;
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

    public BigDecimal getUnitCostSnapshot() {
        return unitCostSnapshot;
    }

    public void setUnitCostSnapshot(BigDecimal unitCostSnapshot) {
        this.unitCostSnapshot = unitCostSnapshot;
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

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}

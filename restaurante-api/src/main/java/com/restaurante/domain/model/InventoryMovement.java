package com.restaurante.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Entidad que representa un movimiento en el kardex de inventario (movimientos_inventario).
 */
@Entity
@Table(name = "movimientos_inventario", schema = "restaurante")
public class InventoryMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "restaurante_id", nullable = false)
    private Long restaurantId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "insumo_id", nullable = false)
    private Supply supply;

    @Column(name = "tipo", nullable = false, length = 30)
    private String type;

    @Column(name = "cantidad", nullable = false, precision = 18, scale = 4)
    private BigDecimal quantity;

    @Column(name = "stock_anterior", precision = 18, scale = 4)
    private BigDecimal previousStock;

    @Column(name = "stock_resultante", precision = 18, scale = 4)
    private BigDecimal resultingStock;

    @Column(name = "costo_unitario_snapshot", precision = 14, scale = 4)
    private BigDecimal unitCostSnapshot;

    @Column(name = "costo_total_snapshot", insertable = false, updatable = false, precision = 18, scale = 4)
    private BigDecimal totalCostSnapshot;

    @Column(name = "entrada_detalle_id")
    private Long entryDetailId;

    @Column(name = "merma_detalle_id")
    private Long wasteDetailId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comanda_detalle_id")
    private ComandaDetail comandaDetail;

    @Column(name = "cancelacion_comanda_detalle_id")
    private Long cancellationDetailId;

    @Column(name = "motivo", nullable = false, length = 500)
    private String reason;

    @Column(name = "usuario_responsable_id", nullable = false)
    private Long responsibleUserId;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant createdAt;

    protected InventoryMovement() {
    }

    public InventoryMovement(Long restaurantId, Supply supply, String type,
                             BigDecimal quantity, ComandaDetail comandaDetail,
                             String reason, Long responsibleUserId) {
        this.restaurantId = restaurantId;
        this.supply = supply;
        this.type = type;
        this.quantity = quantity;
        this.comandaDetail = comandaDetail;
        this.reason = reason;
        this.responsibleUserId = responsibleUserId;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public Long getRestaurantId() {
        return restaurantId;
    }

    public void setRestaurantId(Long restaurantId) {
        this.restaurantId = restaurantId;
    }

    public Supply getSupply() {
        return supply;
    }

    public void setSupply(Supply supply) {
        this.supply = supply;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getPreviousStock() {
        return previousStock;
    }

    public void setPreviousStock(BigDecimal previousStock) {
        this.previousStock = previousStock;
    }

    public BigDecimal getResultingStock() {
        return resultingStock;
    }

    public void setResultingStock(BigDecimal resultingStock) {
        this.resultingStock = resultingStock;
    }

    public BigDecimal getUnitCostSnapshot() {
        return unitCostSnapshot;
    }

    public void setUnitCostSnapshot(BigDecimal unitCostSnapshot) {
        this.unitCostSnapshot = unitCostSnapshot;
    }

    public BigDecimal getTotalCostSnapshot() {
        return totalCostSnapshot;
    }

    public Long getEntryDetailId() {
        return entryDetailId;
    }

    public void setEntryDetailId(Long entryDetailId) {
        this.entryDetailId = entryDetailId;
    }

    public Long getWasteDetailId() {
        return wasteDetailId;
    }

    public void setWasteDetailId(Long wasteDetailId) {
        this.wasteDetailId = wasteDetailId;
    }

    public ComandaDetail getComandaDetail() {
        return comandaDetail;
    }

    public void setComandaDetail(ComandaDetail comandaDetail) {
        this.comandaDetail = comandaDetail;
    }

    public Long getCancellationDetailId() {
        return cancellationDetailId;
    }

    public void setCancellationDetailId(Long cancellationDetailId) {
        this.cancellationDetailId = cancellationDetailId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Long getResponsibleUserId() {
        return responsibleUserId;
    }

    public void setResponsibleUserId(Long responsibleUserId) {
        this.responsibleUserId = responsibleUserId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}

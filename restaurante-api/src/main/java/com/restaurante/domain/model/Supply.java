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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Entidad que representa un insumo (materia prima) del restaurante
 */
@Entity
@Table(name = "insumos", schema = "restaurante")
public class Supply {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "restaurante_id", nullable = false)
    private Long restaurantId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "categoria_insumo_id", nullable = false)
    private SupplyCategory category;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "unidad_stock_id", nullable = false)
    private MeasurementUnit measurementUnit;

    @Column(name = "codigo", nullable = false, length = 30)
    private String code;

    @Column(name = "nombre", nullable = false, length = 120)
    private String name;

    @Column(name = "descripcion", length = 255)
    private String description;

    @Column(name = "costo_unitario_actual", nullable = false, precision = 14, scale = 4)
    private BigDecimal currentUnitCost;

    @Column(name = "stock_actual", nullable = false, precision = 18, scale = 4)
    private BigDecimal currentStock;

    @Column(name = "stock_minimo", nullable = false, precision = 18, scale = 4)
    private BigDecimal minimumStock;

    @Column(name = "stock_maximo", precision = 18, scale = 4)
    private BigDecimal maximumStock;

    @Column(name = "activo", nullable = false)
    private boolean active;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "actualizado_en", nullable = false)
    private Instant updatedAt;

    protected Supply() {
    }

    public Supply(Long restaurantId, SupplyCategory category, MeasurementUnit measurementUnit,
            String code, String name, String description, BigDecimal currentUnitCost,
            BigDecimal minimumStock, BigDecimal maximumStock) {
        this.restaurantId = restaurantId;
        this.category = category;
        this.measurementUnit = measurementUnit;
        this.code = code;
        this.name = name;
        this.description = description;
        this.currentUnitCost = currentUnitCost != null ? currentUnitCost : BigDecimal.ZERO;
        this.currentStock = BigDecimal.ZERO;
        this.minimumStock = minimumStock != null ? minimumStock : BigDecimal.ZERO;
        this.maximumStock = maximumStock;
        this.active = true;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
        if (currentStock == null) {
            currentStock = BigDecimal.ZERO;
        }
        if (minimumStock == null) {
            minimumStock = BigDecimal.ZERO;
        }
        if (currentUnitCost == null) {
            currentUnitCost = BigDecimal.ZERO;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Long getRestaurantId() {
        return restaurantId;
    }

    public SupplyCategory getCategory() {
        return category;
    }

    public void setCategory(SupplyCategory category) {
        this.category = category;
    }

    public MeasurementUnit getMeasurementUnit() {
        return measurementUnit;
    }

    public void setMeasurementUnit(MeasurementUnit measurementUnit) {
        this.measurementUnit = measurementUnit;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getCurrentUnitCost() {
        return currentUnitCost;
    }

    public void setCurrentUnitCost(BigDecimal currentUnitCost) {
        this.currentUnitCost = currentUnitCost;
    }

    public BigDecimal getCurrentStock() {
        return currentStock;
    }

    public void setCurrentStock(BigDecimal currentStock) {
        this.currentStock = currentStock;
    }

    public BigDecimal getMinimumStock() {
        return minimumStock;
    }

    public void setMinimumStock(BigDecimal minimumStock) {
        this.minimumStock = minimumStock;
    }

    public BigDecimal getMaximumStock() {
        return maximumStock;
    }

    public void setMaximumStock(BigDecimal maximumStock) {
        this.maximumStock = maximumStock;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}

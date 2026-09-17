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
 * Entidad que representa un renglón o ingrediente de la receta de un modificador.
 */
@Entity
@Table(name = "receta_modificador_detalles", schema = "restaurante")
public class ModifierRecipeDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "receta_modificador_version_id", nullable = false)
    private ModifierRecipeVersion modifierRecipeVersion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "insumo_id", nullable = false)
    private Supply supply;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "unidad_medida_id", nullable = false)
    private MeasurementUnit measurementUnit;

    @Column(name = "tipo_ajuste", nullable = false, length = 10)
    private String adjustmentType;

    @Column(name = "cantidad", nullable = false, precision = 18, scale = 6)
    private BigDecimal quantity;

    @Column(name = "observacion", length = 255)
    private String notes;

    protected ModifierRecipeDetail() {
    }

    public ModifierRecipeDetail(ModifierRecipeVersion modifierRecipeVersion, Supply supply,
                                MeasurementUnit measurementUnit, String adjustmentType,
                                BigDecimal quantity, String notes) {
        this.modifierRecipeVersion = modifierRecipeVersion;
        this.supply = supply;
        this.measurementUnit = measurementUnit;
        this.adjustmentType = adjustmentType != null ? adjustmentType : "AGREGAR";
        this.quantity = quantity;
        this.notes = notes;
    }

    public Long getId() {
        return id;
    }

    public ModifierRecipeVersion getModifierRecipeVersion() {
        return modifierRecipeVersion;
    }

    public void setModifierRecipeVersion(ModifierRecipeVersion modifierRecipeVersion) {
        this.modifierRecipeVersion = modifierRecipeVersion;
    }

    public Supply getSupply() {
        return supply;
    }

    public void setSupply(Supply supply) {
        this.supply = supply;
    }

    public MeasurementUnit getMeasurementUnit() {
        return measurementUnit;
    }

    public void setMeasurementUnit(MeasurementUnit measurementUnit) {
        this.measurementUnit = measurementUnit;
    }

    public String getAdjustmentType() {
        return adjustmentType;
    }

    public void setAdjustmentType(String adjustmentType) {
        this.adjustmentType = adjustmentType;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}

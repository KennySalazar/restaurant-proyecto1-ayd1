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
 * Entidad que representa un renglón o ingrediente de una receta.
 */
@Entity
@Table(name = "receta_detalles", schema = "restaurante")
public class RecipeDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "receta_version_id", nullable = false)
    private RecipeVersion recipeVersion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "insumo_id", nullable = false)
    private Supply supply;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "unidad_medida_id", nullable = false)
    private MeasurementUnit measurementUnit;

    @Column(name = "cantidad", nullable = false, precision = 18, scale = 6)
    private BigDecimal quantity;

    @Column(name = "observacion", length = 255)
    private String notes;

    protected RecipeDetail() {
    }

    public RecipeDetail(RecipeVersion recipeVersion, Supply supply,
            MeasurementUnit measurementUnit, BigDecimal quantity, String notes) {
        this.recipeVersion = recipeVersion;
        this.supply = supply;
        this.measurementUnit = measurementUnit;
        this.quantity = quantity;
        this.notes = notes;
    }

    public Long getId() {
        return id;
    }

    public RecipeVersion getRecipeVersion() {
        return recipeVersion;
    }

    public void setRecipeVersion(RecipeVersion recipeVersion) {
        this.recipeVersion = recipeVersion;
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

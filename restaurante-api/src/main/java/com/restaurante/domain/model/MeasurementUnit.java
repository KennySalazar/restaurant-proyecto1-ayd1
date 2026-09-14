package com.restaurante.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/**
 * Entidad que representa una unidad de medida para el control de insumos y
 * recetas.
 */
@Entity
@Table(name = "unidades_medida", schema = "restaurante")
public class MeasurementUnit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Short id;

    @Column(name = "codigo", nullable = false, unique = true, length = 20)
    private String code;

    @Column(name = "nombre", nullable = false, length = 60)
    private String name;

    @Column(name = "abreviatura", nullable = false, length = 10)
    private String abbreviation;

    @Column(name = "dimension", nullable = false, length = 15)
    private String dimension;

    @Column(name = "factor_a_base", nullable = false, precision = 18, scale = 6)
    private BigDecimal baseFactor;

    @Column(name = "activo", nullable = false)
    private boolean active;

    protected MeasurementUnit() {
    }

    public MeasurementUnit(Short id, String code, String name, String abbreviation,
            String dimension, BigDecimal baseFactor, boolean active) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.abbreviation = abbreviation;
        this.dimension = dimension;
        this.baseFactor = baseFactor;
        this.active = active;
    }

    public Short getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getAbbreviation() {
        return abbreviation;
    }

    public String getDimension() {
        return dimension;
    }

    public BigDecimal getBaseFactor() {
        return baseFactor;
    }

    public boolean isActive() {
        return active;
    }
}

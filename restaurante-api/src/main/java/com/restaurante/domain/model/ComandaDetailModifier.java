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
 * Entidad que representa un modificador seleccionado para un platillo de una comanda.
 */
@Entity
@Table(name = "comanda_detalle_modificadores", schema = "restaurante")
public class ComandaDetailModifier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "comanda_detalle_id", nullable = false)
    private ComandaDetail comandaDetail;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "modificador_id", nullable = false)
    private Modifier modifier;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receta_modificador_version_id")
    private ModifierRecipeVersion modifierRecipeVersion;

    @Column(name = "nombre_snapshot", nullable = false, length = 120)
    private String nameSnapshot;

    @Column(name = "cantidad", nullable = false)
    private short quantity = 1;

    @Column(name = "precio_adicional_snapshot", nullable = false, precision = 12, scale = 2)
    private BigDecimal additionalPriceSnapshot = BigDecimal.ZERO;

    @Column(name = "costo_adicional_snapshot", nullable = false, precision = 14, scale = 4)
    private BigDecimal additionalCostSnapshot = BigDecimal.ZERO;

    protected ComandaDetailModifier() {
    }

    public ComandaDetailModifier(ComandaDetail comandaDetail, Modifier modifier,
                                 ModifierRecipeVersion modifierRecipeVersion, String nameSnapshot,
                                 short quantity, BigDecimal additionalPriceSnapshot,
                                 BigDecimal additionalCostSnapshot) {
        this.comandaDetail = comandaDetail;
        this.modifier = modifier;
        this.modifierRecipeVersion = modifierRecipeVersion;
        this.nameSnapshot = nameSnapshot;
        this.quantity = quantity > 0 ? quantity : 1;
        this.additionalPriceSnapshot = additionalPriceSnapshot != null ? additionalPriceSnapshot : BigDecimal.ZERO;
        this.additionalCostSnapshot = additionalCostSnapshot != null ? additionalCostSnapshot : BigDecimal.ZERO;
    }

    public Long getId() {
        return id;
    }

    public ComandaDetail getComandaDetail() {
        return comandaDetail;
    }

    public void setComandaDetail(ComandaDetail comandaDetail) {
        this.comandaDetail = comandaDetail;
    }

    public Modifier getModifier() {
        return modifier;
    }

    public void setModifier(Modifier modifier) {
        this.modifier = modifier;
    }

    public ModifierRecipeVersion getModifierRecipeVersion() {
        return modifierRecipeVersion;
    }

    public void setModifierRecipeVersion(ModifierRecipeVersion modifierRecipeVersion) {
        this.modifierRecipeVersion = modifierRecipeVersion;
    }

    public String getNameSnapshot() {
        return nameSnapshot;
    }

    public void setNameSnapshot(String nameSnapshot) {
        this.nameSnapshot = nameSnapshot;
    }

    public short getQuantity() {
        return quantity;
    }

    public void setQuantity(short quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getAdditionalPriceSnapshot() {
        return additionalPriceSnapshot;
    }

    public void setAdditionalPriceSnapshot(BigDecimal additionalPriceSnapshot) {
        this.additionalPriceSnapshot = additionalPriceSnapshot;
    }

    public BigDecimal getAdditionalCostSnapshot() {
        return additionalCostSnapshot;
    }

    public void setAdditionalCostSnapshot(BigDecimal additionalCostSnapshot) {
        this.additionalCostSnapshot = additionalCostSnapshot;
    }
}

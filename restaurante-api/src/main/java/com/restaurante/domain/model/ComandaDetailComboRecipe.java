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
 * Entidad que congela los platillos y recetas que componen un combo incluido en una comanda.
 */
@Entity
@Table(name = "comanda_detalle_combo_recetas", schema = "restaurante")
public class ComandaDetailComboRecipe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "comanda_detalle_id", nullable = false)
    private ComandaDetail comandaDetail;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "platillo_id", nullable = false)
    private Dish dish;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "receta_version_id", nullable = false)
    private RecipeVersion recipeVersion;

    @Column(name = "cantidad_platillo", nullable = false)
    private short dishQuantity;

    @Column(name = "costo_unitario_snapshot", nullable = false, precision = 14, scale = 4)
    private BigDecimal unitCostSnapshot = BigDecimal.ZERO;

    protected ComandaDetailComboRecipe() {
    }

    public ComandaDetailComboRecipe(ComandaDetail comandaDetail, Dish dish,
                                    RecipeVersion recipeVersion, short dishQuantity,
                                    BigDecimal unitCostSnapshot) {
        this.comandaDetail = comandaDetail;
        this.dish = dish;
        this.recipeVersion = recipeVersion;
        this.dishQuantity = dishQuantity;
        this.unitCostSnapshot = unitCostSnapshot != null ? unitCostSnapshot : BigDecimal.ZERO;
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

    public Dish getDish() {
        return dish;
    }

    public void setDish(Dish dish) {
        this.dish = dish;
    }

    public RecipeVersion getRecipeVersion() {
        return recipeVersion;
    }

    public void setRecipeVersion(RecipeVersion recipeVersion) {
        this.recipeVersion = recipeVersion;
    }

    public short getDishQuantity() {
        return dishQuantity;
    }

    public void setDishQuantity(short dishQuantity) {
        this.dishQuantity = dishQuantity;
    }

    public BigDecimal getUnitCostSnapshot() {
        return unitCostSnapshot;
    }

    public void setUnitCostSnapshot(BigDecimal unitCostSnapshot) {
        this.unitCostSnapshot = unitCostSnapshot;
    }
}

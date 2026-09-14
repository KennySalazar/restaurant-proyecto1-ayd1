package com.restaurante.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

/**
 * Entidad de asociación entre un platillo y un modificador disponible.
 */
@Entity
@Table(name = "platillo_modificadores", schema = "restaurante")
public class DishModifier {

    @EmbeddedId
    private DishModifierId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("dishId")
    @JoinColumn(name = "platillo_id", nullable = false)
    private Dish dish;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("modifierId")
    @JoinColumn(name = "modificador_id", nullable = false)
    private Modifier modifier;

    @Column(name = "obligatorio", nullable = false)
    private boolean required;

    @Column(name = "maximo_selecciones", nullable = false)
    private short maxSelections;

    @Column(name = "orden_visual", nullable = false)
    private short visualOrder;

    protected DishModifier() {
    }

    public DishModifier(Dish dish, Modifier modifier, boolean required, short maxSelections, short visualOrder) {
        this.dish = dish;
        this.modifier = modifier;
        this.id = new DishModifierId(dish.getId(), modifier.getId());
        this.required = required;
        this.maxSelections = maxSelections > 0 ? maxSelections : 1;
        this.visualOrder = visualOrder;
    }

    public DishModifier(Dish dish, Modifier modifier) {
        this(dish, modifier, false, (short) 1, (short) 0);
    }

    public DishModifierId getId() {
        return id;
    }

    public void setId(DishModifierId id) {
        this.id = id;
    }

    public Dish getDish() {
        return dish;
    }

    public void setDish(Dish dish) {
        this.dish = dish;
    }

    public Modifier getModifier() {
        return modifier;
    }

    public void setModifier(Modifier modifier) {
        this.modifier = modifier;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public short getMaxSelections() {
        return maxSelections;
    }

    public void setMaxSelections(short maxSelections) {
        this.maxSelections = maxSelections;
    }

    public short getVisualOrder() {
        return visualOrder;
    }

    public void setVisualOrder(short visualOrder) {
        this.visualOrder = visualOrder;
    }
}

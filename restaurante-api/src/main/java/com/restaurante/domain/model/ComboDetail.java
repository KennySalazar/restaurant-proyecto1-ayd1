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

/**
 * Entidad que representa la inclusión de un platillo con su cantidad dentro de un combo.
 */
@Entity
@Table(name = "combo_detalles", schema = "restaurante")
public class ComboDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "combo_id", nullable = false)
    private Combo combo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "platillo_id", nullable = false)
    private Dish dish;

    @Column(name = "cantidad", nullable = false)
    private short quantity;

    @Column(name = "orden_visual", nullable = false)
    private short visualOrder;

    protected ComboDetail() {
    }

    public ComboDetail(Combo combo, Dish dish, short quantity, short visualOrder) {
        this.combo = combo;
        this.dish = dish;
        this.quantity = quantity;
        this.visualOrder = visualOrder;
    }

    public Long getId() {
        return id;
    }

    public Combo getCombo() {
        return combo;
    }

    public void setCombo(Combo combo) {
        this.combo = combo;
    }

    public Dish getDish() {
        return dish;
    }

    public void setDish(Dish dish) {
        this.dish = dish;
    }

    public short getQuantity() {
        return quantity;
    }

    public void setQuantity(short quantity) {
        this.quantity = quantity;
    }

    public short getVisualOrder() {
        return visualOrder;
    }

    public void setVisualOrder(short visualOrder) {
        this.visualOrder = visualOrder;
    }
}

package com.restaurante.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Entidad que representa la categoría del platillo (Entrada, Plato fuerte,
 * Bebida, Postre).
 */
@Entity
@Table(name = "categorias_platillo", schema = "restaurante")
public class DishCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "restaurante_id", nullable = false)
    private Long restaurantId;

    @Column(name = "codigo", nullable = false, length = 30)
    private String code;

    @Column(name = "nombre", nullable = false, length = 80)
    private String name;

    @Column(name = "orden_visual", nullable = false)
    private Short visualOrder;

    protected DishCategory() {
    }

    public DishCategory(Long restaurantId, String code, String name, Short visualOrder) {
        this.restaurantId = restaurantId;
        this.code = code;
        this.name = name;
        this.visualOrder = visualOrder != null ? visualOrder : 0;
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

    public Short getVisualOrder() {
        return visualOrder;
    }

    public void setVisualOrder(Short visualOrder) {
        this.visualOrder = visualOrder;
    }
}

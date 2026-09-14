package com.restaurante.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

/**
 * Clave primaria compuesta para la asociación entre un platillo y un modificador.
 */
@Embeddable
public class DishModifierId implements Serializable {

    @Column(name = "platillo_id")
    private Long dishId;

    @Column(name = "modificador_id")
    private Long modifierId;

    public DishModifierId() {
    }

    public DishModifierId(Long dishId, Long modifierId) {
        this.dishId = dishId;
        this.modifierId = modifierId;
    }

    public Long getDishId() {
        return dishId;
    }

    public void setDishId(Long dishId) {
        this.dishId = dishId;
    }

    public Long getModifierId() {
        return modifierId;
    }

    public void setModifierId(Long modifierId) {
        this.modifierId = modifierId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DishModifierId that = (DishModifierId) o;
        return Objects.equals(dishId, that.dishId) && Objects.equals(modifierId, that.modifierId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dishId, modifierId);
    }
}

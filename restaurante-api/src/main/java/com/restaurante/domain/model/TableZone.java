package com.restaurante.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "zonas_mesa", schema = "restaurante")
public class TableZone {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "restaurante_id", nullable = false)
    private Long restaurantId;

    @Column(name = "nombre", nullable = false, length = 80)
    private String name;

    @Column(name = "descripcion", length = 255)
    private String description;

    @Column(name = "activo", nullable = false)
    private boolean active;

    protected TableZone() {
    }

    public Long getId() {
        return id;
    }

    public Long getRestaurantId() {
        return restaurantId;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public boolean isActive() {
        return active;
    }
}
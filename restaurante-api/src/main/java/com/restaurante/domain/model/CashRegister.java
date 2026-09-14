package com.restaurante.domain.model;

import jakarta.persistence.*;

@Entity
@Table(name = "cajas", schema = "restaurante")
public class CashRegister {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "restaurante_id", nullable = false)
    private Long restaurantId;

    @Column(name = "codigo", nullable = false, length = 30)
    private String code;

    @Column(name = "nombre", nullable = false, length = 100)
    private String name;

    @Column(name = "ubicacion", length = 150)
    private String location;

    @Column(name = "activo", nullable = false)
    private boolean active;

    protected CashRegister() {
    }

    public Long getId() {
        return id;
    }

    public Long getRestaurantId() {
        return restaurantId;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getLocation() {
        return location;
    }

    public boolean isActive() {
        return active;
    }
}
package com.restaurante.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "mesas", schema = "restaurante")
public class RestaurantTable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "restaurante_id", nullable = false)
    private Long restaurantId;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "zona_mesa_id", nullable = false)
    private TableZone zone;

    @Column(name = "numero", nullable = false, length = 20)
    private String number;

    @Column(name = "capacidad", nullable = false)
    private Short capacity;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_actual", nullable = false, length = 20)
    private TableStatus status;

    @Column(name = "activo", nullable = false)
    private boolean active;

    protected RestaurantTable() {
    }

    public RestaurantTable(Long restaurantId, TableZone zone, String number,
                           Short capacity, TableStatus status) {
        this.restaurantId = restaurantId;
        this.zone = zone;
        this.number = number;
        this.capacity = capacity;
        this.status = status;
        this.active = true;
    }

    public Long getId() {
        return id;
    }

    public Long getRestaurantId() {
        return restaurantId;
    }

    public TableZone getZone() {
        return zone;
    }

    public String getNumber() {
        return number;
    }

    public Short getCapacity() {
        return capacity;
    }

    public TableStatus getStatus() {
        return status;
    }

    public boolean isActive() {
        return active;
    }

    public void updateConfiguration(String number, Short capacity, TableZone zone) {
        this.number = number;
        this.capacity = capacity;
        this.zone = zone;
    }
}


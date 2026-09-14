package com.restaurante.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Entidad que representa un platillo del menú del restaurante.
 */
@Entity
@Table(name = "platillos", schema = "restaurante")
public class Dish {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "restaurante_id", nullable = false)
    private Long restaurantId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "categoria_platillo_id", nullable = false)
    private DishCategory category;

    @Column(name = "codigo", nullable = false, length = 30)
    private String code;

    @Column(name = "nombre", nullable = false, length = 120)
    private String name;

    @Column(name = "descripcion", length = 500)
    private String description;

    @Column(name = "precio_venta", nullable = false, precision = 12, scale = 2)
    private BigDecimal salePrice;

    @Column(name = "imagen_url", length = 500)
    private String imageUrl;

    @Column(name = "tiempo_preparacion_minutos", nullable = false)
    private Short preparationTimeMinutes;

    @Column(name = "disponible_manual", nullable = false)
    private boolean manualAvailable;

    @Column(name = "activo", nullable = false)
    private boolean active;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "actualizado_en", nullable = false)
    private Instant updatedAt;

    protected Dish() {
    }

    public Dish(Long restaurantId, DishCategory category, String code, String name,
            String description, BigDecimal salePrice, String imageUrl,
            Short preparationTimeMinutes) {
        this.restaurantId = restaurantId;
        this.category = category;
        this.code = code;
        this.name = name;
        this.description = description;
        this.salePrice = salePrice != null ? salePrice : BigDecimal.ZERO;
        this.imageUrl = imageUrl;
        this.preparationTimeMinutes = preparationTimeMinutes != null ? preparationTimeMinutes : (short) 15;
        this.manualAvailable = true;
        this.active = true;
    }

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        if (this.updatedAt == null) {
            this.updatedAt = now;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
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

    public DishCategory getCategory() {
        return category;
    }

    public void setCategory(DishCategory category) {
        this.category = category;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getSalePrice() {
        return salePrice;
    }

    public void setSalePrice(BigDecimal salePrice) {
        this.salePrice = salePrice;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public Short getPreparationTimeMinutes() {
        return preparationTimeMinutes;
    }

    public void setPreparationTimeMinutes(Short preparationTimeMinutes) {
        this.preparationTimeMinutes = preparationTimeMinutes;
    }

    public boolean isManualAvailable() {
        return manualAvailable;
    }

    public void setManualAvailable(boolean manualAvailable) {
        this.manualAvailable = manualAvailable;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}

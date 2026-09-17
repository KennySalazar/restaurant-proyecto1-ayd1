package com.restaurante.domain.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidad que representa un combo o promoción comercial que agrupa múltiples platillos.
 */
@Entity
@Table(name = "combos", schema = "restaurante")
public class Combo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "restaurante_id", nullable = false)
    private Long restaurantId;

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

    @Column(name = "fecha_inicio")
    private Instant startDate;

    @Column(name = "fecha_fin")
    private Instant endDate;

    @Column(name = "activo", nullable = false)
    private boolean active;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "actualizado_en", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "combo", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("visualOrder ASC")
    private List<ComboDetail> details = new ArrayList<>();

    protected Combo() {
    }

    public Combo(Long restaurantId, String code, String name, String description,
                 BigDecimal salePrice, String imageUrl, Short preparationTimeMinutes,
                 Instant startDate, Instant endDate) {
        this.restaurantId = restaurantId;
        this.code = code;
        this.name = name;
        this.description = description;
        this.salePrice = salePrice != null ? salePrice : BigDecimal.ZERO;
        this.imageUrl = imageUrl;
        this.preparationTimeMinutes = preparationTimeMinutes != null ? preparationTimeMinutes : (short) 15;
        this.manualAvailable = true;
        this.startDate = startDate;
        this.endDate = endDate;
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

    public Instant getStartDate() {
        return startDate;
    }

    public void setStartDate(Instant startDate) {
        this.startDate = startDate;
    }

    public Instant getEndDate() {
        return endDate;
    }

    public void setEndDate(Instant endDate) {
        this.endDate = endDate;
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

    public List<ComboDetail> getDetails() {
        return details;
    }

    public void setDetails(List<ComboDetail> details) {
        this.details = details;
    }

    public void addDetail(ComboDetail detail) {
        details.add(detail);
        detail.setCombo(this);
    }

    public void removeDetail(ComboDetail detail) {
        details.remove(detail);
        detail.setCombo(null);
    }
}

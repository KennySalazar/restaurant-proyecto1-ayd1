package com.restaurante.domain.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidad que representa la cabecera de una merma de inventario (baja por
 * vencimiento, daño, etc.)
 */
@Entity
@Table(name = "mermas", schema = "restaurante")
public class InventoryWaste {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "restaurante_id", nullable = false)
    private Long restaurantId;

    @Column(name = "numero_documento", nullable = false, length = 40)
    private String documentNumber;

    @Column(name = "tipo_motivo", nullable = false, length = 20)
    private String reasonType;

    @Column(name = "motivo_general", nullable = false, length = 500)
    private String generalReason;

    @Column(name = "registrada_por_id", nullable = false)
    private Long registeredById;

    @Column(name = "registrada_en", nullable = false, updatable = false)
    private Instant registeredAt;

    @OneToMany(mappedBy = "waste", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InventoryWasteDetail> details = new ArrayList<>();

    protected InventoryWaste() {
    }

    public InventoryWaste(Long restaurantId, String documentNumber, String reasonType,
            String generalReason, Long registeredById, Instant registeredAt) {
        this.restaurantId = restaurantId;
        this.documentNumber = documentNumber;
        this.reasonType = reasonType;
        this.generalReason = generalReason;
        this.registeredById = registeredById;
        this.registeredAt = registeredAt != null ? registeredAt : Instant.now();
    }

    @PrePersist
    protected void onCreate() {
        if (this.registeredAt == null) {
            this.registeredAt = Instant.now();
        }
    }

    public void addDetail(InventoryWasteDetail detail) {
        this.details.add(detail);
        detail.setWaste(this);
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

    public String getDocumentNumber() {
        return documentNumber;
    }

    public void setDocumentNumber(String documentNumber) {
        this.documentNumber = documentNumber;
    }

    public String getReasonType() {
        return reasonType;
    }

    public void setReasonType(String reasonType) {
        this.reasonType = reasonType;
    }

    public String getGeneralReason() {
        return generalReason;
    }

    public void setGeneralReason(String generalReason) {
        this.generalReason = generalReason;
    }

    public Long getRegisteredById() {
        return registeredById;
    }

    public void setRegisteredById(Long registeredById) {
        this.registeredById = registeredById;
    }

    public Instant getRegisteredAt() {
        return registeredAt;
    }

    public void setRegisteredAt(Instant registeredAt) {
        this.registeredAt = registeredAt;
    }

    public List<InventoryWasteDetail> getDetails() {
        return details;
    }

    public void setDetails(List<InventoryWasteDetail> details) {
        this.details = details;
    }
}

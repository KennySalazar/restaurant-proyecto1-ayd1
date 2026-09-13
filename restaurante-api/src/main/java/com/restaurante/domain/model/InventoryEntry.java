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
 * Entidad que representa un encabezado de entrada de inventario (compra o
 * recepción de mercadería)
 */
@Entity
@Table(name = "entradas_inventario", schema = "restaurante")
public class InventoryEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "restaurante_id", nullable = false)
    private Long restaurantId;

    @Column(name = "numero_documento", nullable = false, length = 40)
    private String documentNumber;

    @Column(name = "fecha_recepcion", nullable = false)
    private Instant receptionDate;

    @Column(name = "proveedor_nombre", length = 150)
    private String supplierName;

    @Column(name = "referencia_compra", length = 100)
    private String purchaseReference;

    @Column(name = "observaciones", length = 500)
    private String notes;

    @Column(name = "recibida_por_id", nullable = false)
    private Long receivedById;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "entry", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InventoryEntryDetail> details = new ArrayList<>();

    protected InventoryEntry() {
    }

    public InventoryEntry(Long restaurantId, String documentNumber, Instant receptionDate,
            String supplierName, String purchaseReference, String notes,
            Long receivedById) {
        this.restaurantId = restaurantId;
        this.documentNumber = documentNumber;
        this.receptionDate = receptionDate != null ? receptionDate : Instant.now();
        this.supplierName = supplierName;
        this.purchaseReference = purchaseReference;
        this.notes = notes;
        this.receivedById = receivedById;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (receptionDate == null) {
            receptionDate = now;
        }
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

    public Instant getReceptionDate() {
        return receptionDate;
    }

    public void setReceptionDate(Instant receptionDate) {
        this.receptionDate = receptionDate;
    }

    public String getSupplierName() {
        return supplierName;
    }

    public void setSupplierName(String supplierName) {
        this.supplierName = supplierName;
    }

    public String getPurchaseReference() {
        return purchaseReference;
    }

    public void setPurchaseReference(String purchaseReference) {
        this.purchaseReference = purchaseReference;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Long getReceivedById() {
        return receivedById;
    }

    public void setReceivedById(Long receivedById) {
        this.receivedById = receivedById;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public List<InventoryEntryDetail> getDetails() {
        return details;
    }

    public void addDetail(InventoryEntryDetail detail) {
        details.add(detail);
        detail.setEntry(this);
    }
}

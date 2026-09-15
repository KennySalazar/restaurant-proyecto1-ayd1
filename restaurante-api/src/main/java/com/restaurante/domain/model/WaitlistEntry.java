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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "lista_espera", schema = "restaurante")
public class WaitlistEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "restaurante_id", nullable = false)
    private Long restaurantId;

    @Column(name = "nombre_cliente", nullable = false, length = 150)
    private String customerName;

    @Column(name = "telefono_cliente", nullable = false, length = 25)
    private String customerPhone;

    @Column(name = "cantidad_personas", nullable = false)
    private Short peopleCount;

    @Column(name = "hora_llegada", nullable = false)
    private OffsetDateTime arrivalTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private WaitlistStatus status;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "mesa_sugerida_id")
    private RestaurantTable suggestedTable;

    @Column(name = "sugerida_en")
    private OffsetDateTime suggestedAt;

    @Column(name = "notificada_en")
    private OffsetDateTime notifiedAt;

    @Column(name = "sentada_en")
    private OffsetDateTime seatedAt;

    @Column(name = "retirada_en")
    private OffsetDateTime removedAt;

    @Column(name = "notas", length = 500)
    private String notes;

    @Column(name = "registrada_por_id")
    private Long registeredById;

    @Column(name = "creado_en", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "actualizado_en", nullable = false)
    private OffsetDateTime updatedAt;

    protected WaitlistEntry() {
    }

    public WaitlistEntry(
            Long restaurantId,
            String customerName,
            String customerPhone,
            Short peopleCount,
            OffsetDateTime arrivalTime,
            String notes,
            Long registeredById) {

        this.restaurantId = restaurantId;
        this.customerName = customerName;
        this.customerPhone = customerPhone;
        this.peopleCount = peopleCount;
        this.arrivalTime = arrivalTime;
        this.status = WaitlistStatus.ESPERANDO;
        this.notes = notes;
        this.registeredById = registeredById;
        this.createdAt = arrivalTime;
        this.updatedAt = arrivalTime;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Long getRestaurantId() {
        return restaurantId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public String getCustomerPhone() {
        return customerPhone;
    }

    public Short getPeopleCount() {
        return peopleCount;
    }

    public OffsetDateTime getArrivalTime() {
        return arrivalTime;
    }

    public WaitlistStatus getStatus() {
        return status;
    }

    public RestaurantTable getSuggestedTable() {
        return suggestedTable;
    }

    public String getNotes() {
        return notes;
    }

    public Long getRegisteredById() {
        return registeredById;
    }

    public void markSeated() {
        this.status = WaitlistStatus.SENTADA;
        if (this.seatedAt == null) {
            this.seatedAt = OffsetDateTime.now();
        }
    }

    public void returnToWaiting() {
        this.status = WaitlistStatus.ESPERANDO;
        this.suggestedTable = null;
    }

    public void setStatus(WaitlistStatus status) {
        this.status = status;
    }

    public void setSuggestedTable(RestaurantTable suggestedTable) {
        this.suggestedTable = suggestedTable;
    }

    public void markSuggested(RestaurantTable table) {
        this.suggestedTable = table;
        this.status = WaitlistStatus.SUGERIDA;
        if (this.suggestedAt == null) {
            this.suggestedAt = OffsetDateTime.now();
        }
    }
}
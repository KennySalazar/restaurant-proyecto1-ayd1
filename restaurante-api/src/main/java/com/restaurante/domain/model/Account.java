package com.restaurante.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Entidad que representa una cuenta activa o histórica de mesa en el restaurante.
 */
@Entity
@Table(name = "cuentas", schema = "restaurante")
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "restaurante_id", nullable = false)
    private Long restaurantId;

    @Column(name = "mesa_id", nullable = false)
    private Long tableId;

    @Column(name = "mesero_id", nullable = false)
    private Long waiterId;

    @Column(name = "cliente_id")
    private Long clientId;

    @Column(name = "reserva_id")
    private Long reservationId;

    @Column(name = "lista_espera_id")
    private Long waitlistId;

    @Column(name = "numero_cuenta", nullable = false, length = 30)
    private String accountNumber;

    @Column(name = "cantidad_personas", nullable = false)
    private short peopleCount = 1;

    @Column(name = "estado", nullable = false, length = 25)
    private String status = "ABIERTA";

    @Column(name = "abierta_en", nullable = false)
    private Instant openedAt;

    @Column(name = "solicitada_cobro_en")
    private Instant requestedPaymentAt;

    @Column(name = "cerrada_en")
    private Instant closedAt;

    @Column(name = "observaciones", length = 500)
    private String notes;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "actualizado_en", nullable = false)
    private Instant updatedAt;

    protected Account() {
    }

    public Account(Long restaurantId, Long tableId, Long waiterId, String accountNumber, short peopleCount) {
        this.restaurantId = restaurantId;
        this.tableId = tableId;
        this.waiterId = waiterId;
        this.accountNumber = accountNumber;
        this.peopleCount = peopleCount > 0 ? peopleCount : 1;
        this.status = "ABIERTA";
        this.openedAt = Instant.now();
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
        if (openedAt == null) {
            openedAt = now;
        }
        if (status == null) {
            status = "ABIERTA";
        }
        if (peopleCount <= 0) {
            peopleCount = 1;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
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

    public Long getTableId() {
        return tableId;
    }

    public void setTableId(Long tableId) {
        this.tableId = tableId;
    }

    public Long getWaiterId() {
        return waiterId;
    }

    public void setWaiterId(Long waiterId) {
        this.waiterId = waiterId;
    }

    public Long getClientId() {
        return clientId;
    }

    public void setClientId(Long clientId) {
        this.clientId = clientId;
    }

    public Long getReservationId() {
        return reservationId;
    }

    public void setReservationId(Long reservationId) {
        this.reservationId = reservationId;
    }

    public Long getWaitlistId() {
        return waitlistId;
    }

    public void setWaitlistId(Long waitlistId) {
        this.waitlistId = waitlistId;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public short getPeopleCount() {
        return peopleCount;
    }

    public void setPeopleCount(short peopleCount) {
        this.peopleCount = peopleCount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getOpenedAt() {
        return openedAt;
    }

    public void setOpenedAt(Instant openedAt) {
        this.openedAt = openedAt;
    }

    public Instant getRequestedPaymentAt() {
        return requestedPaymentAt;
    }

    public void setRequestedPaymentAt(Instant requestedPaymentAt) {
        this.requestedPaymentAt = requestedPaymentAt;
    }

    public Instant getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(Instant closedAt) {
        this.closedAt = closedAt;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}

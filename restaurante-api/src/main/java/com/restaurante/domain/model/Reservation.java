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

import java.time.OffsetDateTime;

@Entity
@Table(name = "reservas", schema = "restaurante")
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "restaurante_id", nullable = false)
    private Long restaurantId;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "mesa_id", nullable = false)
    private RestaurantTable table;

    @Column(name = "codigo_reserva", nullable = false, length = 30)
    private String reservationCode;

    @Column(name = "nombre_cliente", nullable = false, length = 150)
    private String customerName;

    @Column(name = "telefono_cliente", nullable = false, length = 25)
    private String customerPhone;

    @Column(name = "cantidad_personas", nullable = false)
    private Short peopleCount;

    @Column(name = "fecha_hora_inicio", nullable = false)
    private OffsetDateTime startDateTime;

    @Column(name = "fecha_hora_fin", nullable = false)
    private OffsetDateTime endDateTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private ReservationStatus status;

    @Column(name = "notas", length = 500)
    private String notes;

    @Column(name = "creada_por_id")
    private Long createdById;

    protected Reservation() {
    }

    public Reservation(
            Long restaurantId,
            RestaurantTable table,
            String reservationCode,
            String customerName,
            String customerPhone,
            Short peopleCount,
            OffsetDateTime startDateTime,
            OffsetDateTime endDateTime,
            String notes,
            Long createdById) {

        this.restaurantId = restaurantId;
        this.table = table;
        this.reservationCode = reservationCode;
        this.customerName = customerName;
        this.customerPhone = customerPhone;
        this.peopleCount = peopleCount;
        this.startDateTime = startDateTime;
        this.endDateTime = endDateTime;
        this.status = ReservationStatus.PENDIENTE;
        this.notes = notes;
        this.createdById = createdById;
    }

    public Long getId() {
        return id;
    }

    public Long getRestaurantId() {
        return restaurantId;
    }

    public RestaurantTable getTable() {
        return table;
    }

    public String getReservationCode() {
        return reservationCode;
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

    public OffsetDateTime getStartDateTime() {
        return startDateTime;
    }

    public OffsetDateTime getEndDateTime() {
        return endDateTime;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public String getNotes() {
        return notes;
    }

    public Long getCreatedById() {
        return createdById;
    }
}
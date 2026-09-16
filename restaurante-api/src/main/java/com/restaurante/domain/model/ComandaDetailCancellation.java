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
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Entidad que registra la excepción de cancelación de un platillo de comanda.
 */
@Entity
@Table(name = "cancelaciones_comanda_detalle", schema = "restaurante")
public class ComandaDetailCancellation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "comanda_detalle_id", nullable = false)
    private ComandaDetail comandaDetail;

    @Column(name = "tipo", nullable = false, length = 30)
    private String type = "CLIENTE";

    @Column(name = "motivo", nullable = false, length = 500)
    private String reason;

    @Column(name = "estado_solicitud", nullable = false, length = 15)
    private String requestStatus = "APROBADA";

    @Column(name = "accion_inventario", nullable = false, length = 25)
    private String inventoryAction = "REGISTRAR_MERMA";

    @Column(name = "solicitada_por_id", nullable = false)
    private Long requestedById;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "solicitada_por_id", insertable = false, updatable = false)
    private RestaurantUserProfile requestedByUser;

    @Column(name = "autorizada_por_id")
    private Long authorizedById;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "autorizada_por_id", insertable = false, updatable = false)
    private RestaurantUserProfile authorizedByUser;

    @Column(name = "solicitada_en", nullable = false)
    private Instant requestedAt;

    @Column(name = "resuelta_en")
    private Instant resolvedAt;

    protected ComandaDetailCancellation() {
    }

    public ComandaDetailCancellation(
            ComandaDetail comandaDetail,
            String type,
            String reason,
            String requestStatus,
            String inventoryAction,
            Long requestedById,
            Long authorizedById) {
        this.comandaDetail = comandaDetail;
        this.type = (type != null && !type.isBlank()) ? type : "CLIENTE";
        this.reason = reason;
        this.requestStatus = (requestStatus != null && !requestStatus.isBlank()) ? requestStatus : "APROBADA";
        this.inventoryAction = (inventoryAction != null && !inventoryAction.isBlank()) ? inventoryAction : "REGISTRAR_MERMA";
        this.requestedById = requestedById;
        this.authorizedById = authorizedById;
        this.requestedAt = Instant.now();
        if ("APROBADA".equalsIgnoreCase(this.requestStatus) || "RECHAZADA".equalsIgnoreCase(this.requestStatus)) {
            this.resolvedAt = this.requestedAt;
        }
    }

    @PrePersist
    protected void onCreate() {
        if (this.requestedAt == null) {
            this.requestedAt = Instant.now();
        }
        if (("APROBADA".equalsIgnoreCase(this.requestStatus) || "RECHAZADA".equalsIgnoreCase(this.requestStatus)) && this.resolvedAt == null) {
            this.resolvedAt = this.requestedAt;
        }
    }

    public Long getId() {
        return id;
    }

    public ComandaDetail getComandaDetail() {
        return comandaDetail;
    }

    public void setComandaDetail(ComandaDetail comandaDetail) {
        this.comandaDetail = comandaDetail;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getRequestStatus() {
        return requestStatus;
    }

    public void setRequestStatus(String requestStatus) {
        this.requestStatus = requestStatus;
    }

    public String getInventoryAction() {
        return inventoryAction;
    }

    public void setInventoryAction(String inventoryAction) {
        this.inventoryAction = inventoryAction;
    }

    public Long getRequestedById() {
        return requestedById;
    }

    public void setRequestedById(Long requestedById) {
        this.requestedById = requestedById;
    }

    public RestaurantUserProfile getRequestedByUser() {
        return requestedByUser;
    }

    public Long getAuthorizedById() {
        return authorizedById;
    }

    public void setAuthorizedById(Long authorizedById) {
        this.authorizedById = authorizedById;
    }

    public RestaurantUserProfile getAuthorizedByUser() {
        return authorizedByUser;
    }

    public Instant getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(Instant requestedAt) {
        this.requestedAt = requestedAt;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(Instant resolvedAt) {
        this.resolvedAt = resolvedAt;
    }
}

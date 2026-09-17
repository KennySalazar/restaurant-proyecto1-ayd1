package com.restaurante.domain.model;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidad que representa una comanda u orden emitida por un mesero para una cuenta.
 */
@Entity
@Table(name = "comandas", schema = "restaurante")
public class Comanda {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cuenta_id", nullable = false)
    private Account account;

    @Column(name = "numero_ronda", nullable = false)
    private short roundNumber = 1;

    @Column(name = "mesero_id", nullable = false)
    private Long waiterId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mesero_id", insertable = false, updatable = false)
    private RestaurantUserProfile waiterUser;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private ComandaStatus status = ComandaStatus.BORRADOR;

    @Column(name = "creada_en", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "enviada_en")
    private Instant sentAt;

    @Column(name = "finalizada_en")
    private Instant finishedAt;

    @Column(name = "notas_generales", length = 500)
    private String generalNotes;

    @OneToMany(mappedBy = "comanda", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ComandaDetail> details = new ArrayList<>();

    protected Comanda() {
    }

    public Comanda(Account account, short roundNumber, Long waiterId, String generalNotes) {
        this.account = account;
        this.roundNumber = roundNumber > 0 ? roundNumber : 1;
        this.waiterId = waiterId;
        this.status = ComandaStatus.BORRADOR;
        this.generalNotes = generalNotes;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (status == null) {
            status = ComandaStatus.BORRADOR;
        }
        if (roundNumber <= 0) {
            roundNumber = 1;
        }
    }

    public Long getId() {
        return id;
    }

    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }

    public short getRoundNumber() {
        return roundNumber;
    }

    public void setRoundNumber(short roundNumber) {
        this.roundNumber = roundNumber;
    }

    public Long getWaiterId() {
        return waiterId;
    }

    public void setWaiterId(Long waiterId) {
        this.waiterId = waiterId;
    }

    public ComandaStatus getStatus() {
        return status;
    }

    public void setStatus(ComandaStatus status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getSentAt() {
        return sentAt;
    }

    public void setSentAt(Instant sentAt) {
        this.sentAt = sentAt;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(Instant finishedAt) {
        this.finishedAt = finishedAt;
    }

    public String getGeneralNotes() {
        return generalNotes;
    }

    public void setGeneralNotes(String generalNotes) {
        this.generalNotes = generalNotes;
    }

    public List<ComandaDetail> getDetails() {
        return details;
    }

    public void setDetails(List<ComandaDetail> details) {
        this.details = details;
    }

    public void addDetail(ComandaDetail detail) {
        details.add(detail);
        detail.setComanda(this);
    }

    public RestaurantUserProfile getWaiterUser() {
        return waiterUser;
    }

    public void setWaiterUser(RestaurantUserProfile waiterUser) {
        this.waiterUser = waiterUser;
    }
}

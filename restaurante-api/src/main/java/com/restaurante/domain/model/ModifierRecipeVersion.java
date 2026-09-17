package com.restaurante.domain.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * Entidad que representa una versión de la receta de un modificador.
 */
@Entity
@Table(name = "receta_modificador_versiones", schema = "restaurante")
public class ModifierRecipeVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "modificador_id", nullable = false)
    private Modifier modifier;

    @Column(name = "numero_version", nullable = false)
    private Integer versionNumber;

    @Column(name = "estado", nullable = false, length = 15)
    private String status;

    @Column(name = "motivo_cambio", length = 500)
    private String changeReason;

    @Column(name = "vigente_desde")
    private Instant effectiveFrom;

    @Column(name = "vigente_hasta")
    private Instant effectiveTo;

    @Column(name = "creado_por_id")
    private Long createdById;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "modifierRecipeVersion", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ModifierRecipeDetail> details = new ArrayList<>();

    protected ModifierRecipeVersion() {
    }

    public ModifierRecipeVersion(Modifier modifier, Integer versionNumber, String status,
                                 String changeReason, Long createdById) {
        this.modifier = modifier;
        this.versionNumber = versionNumber != null ? versionNumber : 1;
        this.status = status != null ? status : "BORRADOR";
        this.changeReason = changeReason;
        this.createdById = createdById;
        this.createdAt = Instant.now();
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
    }

    public void addDetail(ModifierRecipeDetail detail) {
        this.details.add(detail);
        detail.setModifierRecipeVersion(this);
    }

    public Long getId() {
        return id;
    }

    public Modifier getModifier() {
        return modifier;
    }

    public void setModifier(Modifier modifier) {
        this.modifier = modifier;
    }

    public Integer getVersionNumber() {
        return versionNumber;
    }

    public void setVersionNumber(Integer versionNumber) {
        this.versionNumber = versionNumber;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getChangeReason() {
        return changeReason;
    }

    public void setChangeReason(String changeReason) {
        this.changeReason = changeReason;
    }

    public Instant getEffectiveFrom() {
        return effectiveFrom;
    }

    public void setEffectiveFrom(Instant effectiveFrom) {
        this.effectiveFrom = effectiveFrom;
    }

    public Instant getEffectiveTo() {
        return effectiveTo;
    }

    public void setEffectiveTo(Instant effectiveTo) {
        this.effectiveTo = effectiveTo;
    }

    public Long getCreatedById() {
        return createdById;
    }

    public void setCreatedById(Long createdById) {
        this.createdById = createdById;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public List<ModifierRecipeDetail> getDetails() {
        return details;
    }

    public void setDetails(List<ModifierRecipeDetail> details) {
        this.details = details;
    }
}

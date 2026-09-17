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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidad que representa un renglón o ítem incluido en una comanda (platillo o combo).
 */
@Entity
@Table(name = "comanda_detalles", schema = "restaurante")
public class ComandaDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "comanda_id", nullable = false)
    private Comanda comanda;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "platillo_id")
    private Dish dish;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "combo_id")
    private Combo combo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receta_version_id")
    private RecipeVersion recipeVersion;

    @Column(name = "nombre_snapshot", nullable = false, length = 150)
    private String nameSnapshot;

    @Column(name = "cantidad", nullable = false)
    private short quantity;

    @Column(name = "precio_unitario_snapshot", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPriceSnapshot;

    @Column(name = "costo_unitario_snapshot", nullable = false, precision = 14, scale = 4)
    private BigDecimal unitCostSnapshot = BigDecimal.ZERO;

    @Column(name = "tiempo_estimado_minutos", nullable = false)
    private short estimatedTimeMinutes;

    @Column(name = "notas_especiales", length = 500)
    private String specialNotes;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private ComandaDetailStatus status = ComandaDetailStatus.BORRADOR;

    @Column(name = "recibido_en")
    private Instant receivedAt;

    @Column(name = "preparacion_iniciada_en")
    private Instant preparationStartedAt;

    @Column(name = "listo_en")
    private Instant readyAt;

    @Column(name = "entregado_en")
    private Instant deliveredAt;

    @Column(name = "actualizado_por_id")
    private Long updatedById;

    @Column(name = "actualizado_en", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "comandaDetail", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ComandaDetailModifier> modifiers = new ArrayList<>();

    @OneToMany(mappedBy = "comandaDetail", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ComandaDetailComboRecipe> comboRecipes = new ArrayList<>();

    protected ComandaDetail() {
    }

    public ComandaDetail(Comanda comanda, Dish dish, RecipeVersion recipeVersion,
                         String nameSnapshot, short quantity, BigDecimal unitPriceSnapshot,
                         BigDecimal unitCostSnapshot, short estimatedTimeMinutes, String specialNotes) {
        this.comanda = comanda;
        this.dish = dish;
        this.recipeVersion = recipeVersion;
        this.nameSnapshot = nameSnapshot;
        this.quantity = quantity;
        this.unitPriceSnapshot = unitPriceSnapshot;
        this.unitCostSnapshot = unitCostSnapshot != null ? unitCostSnapshot : BigDecimal.ZERO;
        this.estimatedTimeMinutes = estimatedTimeMinutes > 0 ? estimatedTimeMinutes : (short) 15;
        this.specialNotes = specialNotes;
        this.status = ComandaDetailStatus.BORRADOR;
    }

    public ComandaDetail(Comanda comanda, Combo combo,
                         String nameSnapshot, short quantity, BigDecimal unitPriceSnapshot,
                         BigDecimal unitCostSnapshot, short estimatedTimeMinutes, String specialNotes) {
        this.comanda = comanda;
        this.combo = combo;
        this.nameSnapshot = nameSnapshot;
        this.quantity = quantity;
        this.unitPriceSnapshot = unitPriceSnapshot;
        this.unitCostSnapshot = unitCostSnapshot != null ? unitCostSnapshot : BigDecimal.ZERO;
        this.estimatedTimeMinutes = estimatedTimeMinutes > 0 ? estimatedTimeMinutes : (short) 15;
        this.specialNotes = specialNotes;
        this.status = ComandaDetailStatus.BORRADOR;
    }

    @PrePersist
    void onCreate() {
        if (updatedAt == null) {
            updatedAt = Instant.now();
        }
        if (status == null) {
            status = ComandaDetailStatus.BORRADOR;
        }
        if (unitCostSnapshot == null) {
            unitCostSnapshot = BigDecimal.ZERO;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Comanda getComanda() {
        return comanda;
    }

    public void setComanda(Comanda comanda) {
        this.comanda = comanda;
    }

    public Dish getDish() {
        return dish;
    }

    public void setDish(Dish dish) {
        this.dish = dish;
    }

    public Combo getCombo() {
        return combo;
    }

    public void setCombo(Combo combo) {
        this.combo = combo;
    }

    public RecipeVersion getRecipeVersion() {
        return recipeVersion;
    }

    public void setRecipeVersion(RecipeVersion recipeVersion) {
        this.recipeVersion = recipeVersion;
    }

    public String getNameSnapshot() {
        return nameSnapshot;
    }

    public void setNameSnapshot(String nameSnapshot) {
        this.nameSnapshot = nameSnapshot;
    }

    public short getQuantity() {
        return quantity;
    }

    public void setQuantity(short quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getUnitPriceSnapshot() {
        return unitPriceSnapshot;
    }

    public void setUnitPriceSnapshot(BigDecimal unitPriceSnapshot) {
        this.unitPriceSnapshot = unitPriceSnapshot;
    }

    public BigDecimal getUnitCostSnapshot() {
        return unitCostSnapshot;
    }

    public void setUnitCostSnapshot(BigDecimal unitCostSnapshot) {
        this.unitCostSnapshot = unitCostSnapshot;
    }

    public short getEstimatedTimeMinutes() {
        return estimatedTimeMinutes;
    }

    public void setEstimatedTimeMinutes(short estimatedTimeMinutes) {
        this.estimatedTimeMinutes = estimatedTimeMinutes;
    }

    public String getSpecialNotes() {
        return specialNotes;
    }

    public void setSpecialNotes(String specialNotes) {
        this.specialNotes = specialNotes;
    }

    public ComandaDetailStatus getStatus() {
        return status;
    }

    public void setStatus(ComandaDetailStatus status) {
        this.status = status;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }

    public void setReceivedAt(Instant receivedAt) {
        this.receivedAt = receivedAt;
    }

    public Instant getPreparationStartedAt() {
        return preparationStartedAt;
    }

    public void setPreparationStartedAt(Instant preparationStartedAt) {
        this.preparationStartedAt = preparationStartedAt;
    }

    public Instant getReadyAt() {
        return readyAt;
    }

    public void setReadyAt(Instant readyAt) {
        this.readyAt = readyAt;
    }

    public Instant getDeliveredAt() {
        return deliveredAt;
    }

    public void setDeliveredAt(Instant deliveredAt) {
        this.deliveredAt = deliveredAt;
    }

    public Long getUpdatedById() {
        return updatedById;
    }

    public void setUpdatedById(Long updatedById) {
        this.updatedById = updatedById;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public List<ComandaDetailModifier> getModifiers() {
        return modifiers;
    }

    public void setModifiers(List<ComandaDetailModifier> modifiers) {
        this.modifiers = modifiers;
    }

    public void addModifier(ComandaDetailModifier modifier) {
        modifiers.add(modifier);
        modifier.setComandaDetail(this);
    }

    public List<ComandaDetailComboRecipe> getComboRecipes() {
        return comboRecipes;
    }

    public void setComboRecipes(List<ComandaDetailComboRecipe> comboRecipes) {
        this.comboRecipes = comboRecipes;
    }
}

package com.restaurante.domain.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneId;

@Entity
@Table(
        name = "configuraciones_restaurante",
        schema = "restaurante"
)
public class RestaurantConfiguration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "restaurante_id", nullable = false)
    private Long restaurantId;

    @Column(name = "nombre_impuesto", nullable = false, length = 80)
    private String taxName;

    @Column(name = "porcentaje_impuesto", nullable = false, precision = 7, scale = 4)
    private BigDecimal taxPercentage;

    @Column(name = "porcentaje_propina", nullable = false, precision = 7, scale = 4)
    private BigDecimal tipPercentage;

    @Column(name = "propina_editable", nullable = false)
    private boolean editableTip;

    @Column(name = "puntos_por_moneda", nullable = false, precision = 12, scale = 4)
    private BigDecimal pointsPerCurrency;

    @Column(name = "valor_monetario_punto", nullable = false, precision = 12, scale = 4)
    private BigDecimal pointMonetaryValue;

    @Column(name = "duracion_reserva_minutos", nullable = false)
    private Short reservationDurationMinutes;

    @Column(name = "tolerancia_reserva_minutos", nullable = false)
    private Short reservationToleranceMinutes;

    @Column(name = "vigente_desde", nullable = false)
    private OffsetDateTime validFrom;

    @Column(name = "vigente_hasta")
    private OffsetDateTime validUntil;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 15)
    private RestaurantConfigurationStatus status;

    @Column(name = "creado_por_id")
    private Long createdById;

    protected RestaurantConfiguration() {
    }

    public RestaurantConfiguration(
            Long restaurantId,
            String taxName,
            BigDecimal taxPercentage,
            BigDecimal tipPercentage,
            boolean editableTip,
            BigDecimal pointsPerCurrency,
            BigDecimal pointMonetaryValue,
            Short reservationDurationMinutes,
            Short reservationToleranceMinutes,
            Long createdById) {

        this.restaurantId = restaurantId;
        this.taxName = taxName;
        this.taxPercentage = taxPercentage;
        this.tipPercentage = tipPercentage;
        this.editableTip = editableTip;
        this.pointsPerCurrency = pointsPerCurrency;
        this.pointMonetaryValue = pointMonetaryValue;
        this.reservationDurationMinutes = reservationDurationMinutes;
        this.reservationToleranceMinutes = reservationToleranceMinutes;
        this.validFrom = OffsetDateTime.now(
                ZoneId.of("America/Guatemala")
        );
        this.status = RestaurantConfigurationStatus.VIGENTE;
        this.createdById = createdById;
    }

    public void markHistorical(OffsetDateTime validUntil) {
        this.status = RestaurantConfigurationStatus.HISTORICA;
        this.validUntil = validUntil;
    }

    public Long getId() {
        return id;
    }

    public Long getRestaurantId() {
        return restaurantId;
    }

    public String getTaxName() {
        return taxName;
    }

    public BigDecimal getTaxPercentage() {
        return taxPercentage;
    }

    public BigDecimal getTipPercentage() {
        return tipPercentage;
    }

    public boolean isEditableTip() {
        return editableTip;
    }

    public BigDecimal getPointsPerCurrency() {
        return pointsPerCurrency;
    }

    public BigDecimal getPointMonetaryValue() {
        return pointMonetaryValue;
    }

    public Short getReservationDurationMinutes() {
        return reservationDurationMinutes;
    }

    public Short getReservationToleranceMinutes() {
        return reservationToleranceMinutes;
    }

    public OffsetDateTime getValidFrom() {
        return validFrom;
    }

    public OffsetDateTime getValidUntil() {
        return validUntil;
    }

    public RestaurantConfigurationStatus getStatus() {
        return status;
    }

    public Long getCreatedById() {
        return createdById;
    }
}
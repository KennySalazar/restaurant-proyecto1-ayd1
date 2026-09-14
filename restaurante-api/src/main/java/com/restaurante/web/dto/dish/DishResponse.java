package com.restaurante.web.dto.dish;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Información comercial, operativa y disponibilidad actual de un platillo registrado en el menú.
 */
@Schema(description = "Información consolidada y disponibilidad actual de un platillo en el catálogo")
public record DishResponse(
        @Schema(description = "Identificador único del platillo", example = "1")
        Long id,

        @Schema(description = "Código del platillo", example = "PLA-0001")
        String code,

        @Schema(description = "Nombre del platillo", example = "Hamburguesa Clásica")
        String name,

        @Schema(description = "Descripción del platillo", example = "Deliciosa hamburguesa con carne de res, lechuga y tomate")
        String description,

        @Schema(description = "Identificador de la categoría", example = "2")
        Long categoryId,

        @Schema(description = "Nombre de la categoría", example = "Plato fuerte")
        String categoryName,

        @Schema(description = "Precio de venta fijado", example = "45.00")
        BigDecimal salePrice,

        @Schema(description = "URL de la imagen del platillo", example = "https://images.restaurante.com/dishes/hamburguesa.jpg")
        String imageUrl,

        @Schema(description = "Tiempo estimado de preparación en minutos", example = "15")
        Short preparationTimeMinutes,

        @Schema(description = "Disponibilidad operativa actual del platillo para venta", example = "true")
        @JsonAlias({"isAvailable"})
        Boolean available,

        @Schema(description = "Cantidad máxima de porciones disponibles para preparar según inventario actual", example = "12")
        Integer availablePortions,

        @Schema(description = "Código del motivo de falta de disponibilidad (MANUAL, FALTA_INSUMOS, INACTIVO) o null si está disponible", example = "FALTA_INSUMOS")
        String unavailabilityReason,

        @Schema(description = "Descripción legible del motivo de falta de disponibilidad o null si está disponible", example = "Falta de insumos")
        String unavailabilityReasonDescription,

        @Schema(description = "Disponibilidad manual configurada por el administrador en el menú", example = "true")
        Boolean manualAvailable,

        @Schema(description = "Estado activo del platillo en el catálogo", example = "true")
        Boolean active,

        @Schema(description = "Fecha de registro del platillo", example = "2026-09-14T00:00:00Z")
        Instant createdAt,

        @Schema(description = "Fecha de última actualización", example = "2026-09-14T00:00:00Z")
        Instant updatedAt
) {

    @JsonProperty("disponible")
    public Boolean getDisponible() {
        return available;
    }

    @JsonProperty("porcionesDisponibles")
    public Integer getPorcionesDisponibles() {
        return availablePortions;
    }

    @JsonProperty("motivoIndisponibilidad")
    public String getMotivoIndisponibilidad() {
        return unavailabilityReasonDescription != null ? unavailabilityReasonDescription : unavailabilityReason;
    }
}

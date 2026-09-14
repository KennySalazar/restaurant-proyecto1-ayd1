package com.restaurante.web.dto.combo;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Información comercial y operativa consolidada de un combo o promoción.
 */
@Schema(description = "Información consolidada de un combo o promoción")
public record ComboResponse(
        @Schema(description = "Identificador único del combo", example = "1")
        Long id,

        @Schema(description = "Código del combo", example = "COM-0001")
        String code,

        @Schema(description = "Nombre comercial del combo", example = "Combo Pareja")
        String name,

        @Schema(description = "Descripción del combo", example = "Incluye 2 hamburguesas clásicas y 2 bebidas")
        String description,

        @Schema(description = "Precio especial de venta fijado para el combo", example = "75.00")
        BigDecimal salePrice,

        @Schema(description = "Suma referencial de los precios regulares individuales de los platillos incluidos", example = "90.00")
        BigDecimal regularPriceSum,

        @Schema(description = "Ahorro estimado para el cliente respecto al precio regular", example = "15.00")
        BigDecimal estimatedSavings,

        @Schema(description = "URL de la imagen del combo", example = "https://images.restaurante.com/combos/combo-pareja.jpg")
        String imageUrl,

        @Schema(description = "Tiempo estimado de preparación en minutos", example = "20")
        Short preparationTimeMinutes,

        @Schema(description = "Disponibilidad manual en el menú", example = "true")
        Boolean manualAvailable,

        @Schema(description = "Fecha de inicio de vigencia (opcional)", example = "2026-09-14T00:00:00Z")
        Instant startDate,

        @Schema(description = "Fecha de fin de vigencia (opcional)", example = "2026-12-31T23:59:59Z")
        Instant endDate,

        @Schema(description = "Estado activo del combo", example = "true")
        Boolean active,

        @Schema(description = "Listado de platillos incluidos en el combo")
        List<ComboDetailResponse> items,

        @Schema(description = "Fecha de registro", example = "2026-09-14T00:00:00Z")
        Instant createdAt,

        @Schema(description = "Fecha de última actualización", example = "2026-09-14T00:00:00Z")
        Instant updatedAt
) {
}

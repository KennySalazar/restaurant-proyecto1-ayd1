package com.restaurante.web.dto.combo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Solicitud para registrar un nuevo combo o promoción en el menú.
 */
@Schema(description = "Solicitud para registrar un nuevo combo o promoción")
public record CreateComboRequest(
        @Schema(description = "Código del combo (opcional, auto-generado si no se provee)", example = "COM-0001")
        @Size(max = 30, message = "El código no puede exceder 30 caracteres")
        String code,

        @Schema(description = "Nombre del combo o promoción", example = "Combo Pareja", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "El nombre del combo es obligatorio")
        @Size(max = 120, message = "El nombre no puede exceder 120 caracteres")
        String name,

        @Schema(description = "Descripción comercial del combo", example = "Incluye 2 hamburguesas clásicas y 2 bebidas")
        @Size(max = 500, message = "La descripción no puede exceder 500 caracteres")
        String description,

        @Schema(description = "Precio especial de venta del combo (mayor a cero)", example = "75.00", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "El precio especial es obligatorio")
        @DecimalMin(value = "0.01", message = "El precio debe ser mayor que cero")
        BigDecimal salePrice,

        @Schema(description = "URL de la imagen del combo", example = "https://images.restaurante.com/combos/combo-pareja.jpg")
        @Size(max = 500, message = "La URL de la imagen no puede exceder 500 caracteres")
        String imageUrl,

        @Schema(description = "Tiempo estimado de preparación en minutos (opcional)", example = "20")
        @Min(value = 1, message = "El tiempo de preparación debe ser mayor que cero")
        Short preparationTimeMinutes,

        @Schema(description = "Fecha de inicio de la vigencia de la promoción (opcional)", example = "2026-09-14T00:00:00Z")
        Instant startDate,

        @Schema(description = "Fecha de fin de la vigencia de la promoción (opcional)", example = "2026-12-31T23:59:59Z")
        Instant endDate,

        @Schema(description = "Listado de platillos y cantidades a incluir (al menos dos platillos)", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "La lista de platillos del combo es obligatoria")
        @Size(min = 2, message = "El combo debe incluir al menos dos platillos")
        @Valid
        List<ComboItemRequest> items
) {
}

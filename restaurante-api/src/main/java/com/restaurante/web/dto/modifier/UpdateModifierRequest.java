package com.restaurante.web.dto.modifier;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/**
 * Solicitud para actualizar un modificador de platillos.
 */
@Schema(description = "Solicitud para actualizar un modificador de platillos")
public record UpdateModifierRequest(
        @Schema(description = "Código del modificador (opcional)", example = "MOD-0001")
        @Size(max = 30, message = "El código no puede exceder 30 caracteres")
        String code,

        @Schema(description = "Nombre comercial del modificador", example = "Queso cheddar extra", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "El nombre del modificador es obligatorio")
        @Size(max = 100, message = "El nombre no puede exceder 100 caracteres")
        String name,

        @Schema(description = "Descripción del modificador", example = "Doble queso cheddar")
        @Size(max = 255, message = "La descripción no puede exceder 255 caracteres")
        String description,

        @Schema(description = "Costo o precio adicional (no puede ser negativo)", example = "7.50")
        @DecimalMin(value = "0.00", message = "El costo adicional no puede ser negativo")
        BigDecimal additionalPrice,

        @Schema(description = "Listado actualizado de identificadores de platillos asociados (opcional)", example = "[1, 3]")
        List<Long> dishIds
) {
}

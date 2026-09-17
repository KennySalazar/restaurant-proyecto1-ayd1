package com.restaurante.web.dto.modifier;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

/**
 * Solicitud para registrar un nuevo modificador de platillos.
 */
@Schema(description = "Solicitud para registrar un nuevo modificador de platillos")
public record CreateModifierRequest(
        @Schema(description = "Código del modificador (opcional, auto-generado si no se envía)", example = "MOD-0001")
        @Size(max = 30, message = "El código no puede exceder 30 caracteres")
        String code,

        @Schema(description = "Nombre comercial del modificador", example = "Queso extra", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "El nombre del modificador es obligatorio")
        @Size(max = 100, message = "El nombre no puede exceder 100 caracteres")
        String name,

        @Schema(description = "Descripción del modificador", example = "Porción adicional de queso cheddar derretido")
        @Size(max = 255, message = "La descripción no puede exceder 255 caracteres")
        String description,

        @Schema(description = "Costo o precio adicional fijado (0 o positivo). Si no se indica, es 0", example = "5.00")
        @DecimalMin(value = "0.00", message = "El costo adicional no puede ser negativo")
        BigDecimal additionalPrice,

        @Schema(description = "Listado de identificadores de platillos asociados", example = "[1, 2]", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotEmpty(message = "Debe asociar el modificador al menos con un platillo")
        List<Long> dishIds
) {
}

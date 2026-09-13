package com.restaurante.web.dto.supply;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@Schema(description = "Solicitud para actualizar la información de un insumo registrado")
public record UpdateSupplyRequest(
                @Schema(description = "Código único del insumo (opcional)", example = "INS-0001") @Size(max = 30, message = "El código no puede exceder 30 caracteres") String code,

                @Schema(description = "Nombre del insumo", example = "Harina de Trigo Integral", requiredMode = Schema.RequiredMode.REQUIRED) @NotBlank(message = "El nombre del insumo es obligatorio") @Size(max = 120, message = "El nombre no puede exceder 120 caracteres") String name,

                @Schema(description = "Descripción detallada del insumo", example = "Harina 100% integral para panificación") @Size(max = 255, message = "La descripción no puede exceder 255 caracteres") String description,

                @Schema(description = "Identificador de la categoría del insumo", example = "4", requiredMode = Schema.RequiredMode.REQUIRED) @NotNull(message = "La categoría del insumo es obligatoria") Long categoryId,

                @Schema(description = "Identificador de la unidad de medida", example = "2", requiredMode = Schema.RequiredMode.REQUIRED) @NotNull(message = "La unidad de medida es obligatoria") Short unitId,

                @Schema(description = "Costo unitario de compra", example = "16.00", requiredMode = Schema.RequiredMode.REQUIRED) @NotNull(message = "El costo unitario de compra es obligatorio") @DecimalMin(value = "0.0", inclusive = true, message = "El costo unitario de compra no puede ser negativo") BigDecimal unitCost,

                @Schema(description = "Nivel de stock mínimo para alertas", example = "6.00") @DecimalMin(value = "0.0", inclusive = true, message = "El stock mínimo no puede ser negativo") BigDecimal minimumStock,

                @Schema(description = "Nivel de stock máximo de almacenamiento", example = "60.00") @DecimalMin(value = "0.0", inclusive = true, message = "El stock máximo no puede ser negativo") BigDecimal maximumStock) {
}

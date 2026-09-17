package com.restaurante.web.dto.supply;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;

@Schema(description = "Detalle del insumo registrado en el catálogo")
public record SupplyResponse(
                @Schema(description = "Identificador único del insumo", example = "1") Long id,

                @Schema(description = "Identificador del restaurante", example = "1") Long restaurantId,

                @Schema(description = "Código del insumo", example = "INS-0001") String code,

                @Schema(description = "Nombre del insumo", example = "Harina de Trigo") String name,

                @Schema(description = "Descripción del insumo", example = "Harina de fuerza para preparación de masa") String description,

                @Schema(description = "Identificador de la categoría", example = "4") Long categoryId,

                @Schema(description = "Nombre de la categoría", example = "Abarrote") String categoryName,

                @Schema(description = "Identificador de la unidad de medida", example = "2") Short unitId,

                @Schema(description = "Nombre de la unidad de medida", example = "Kilogramo") String unitName,

                @Schema(description = "Abreviatura de la unidad de medida", example = "kg") String unitAbbreviation,

                @Schema(description = "Costo unitario actual de compra", example = "15.5000") BigDecimal unitCost,

                @Schema(description = "Existencia actual en inventario", example = "0.0000") BigDecimal currentStock,

                @Schema(description = "Stock mínimo para alertas", example = "5.0000") BigDecimal minimumStock,

                @Schema(description = "Capacidad máxima de almacenamiento", example = "50.0000") BigDecimal maximumStock,

                @Schema(description = "Indica si el insumo está activo", example = "true") boolean active,

                @Schema(description = "Fecha de registro del insumo", example = "2026-09-13T10:00:00Z") Instant createdAt) {
}

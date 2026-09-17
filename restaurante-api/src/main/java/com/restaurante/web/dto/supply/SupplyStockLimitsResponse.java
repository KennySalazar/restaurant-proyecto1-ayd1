package com.restaurante.web.dto.supply;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Respuesta de confirmación tras configurar los límites de stock de un insumo")
public record SupplyStockLimitsResponse(
        @Schema(description = "Mensaje de confirmación", example = "Límites de stock configurados exitosamente")
        String message,

        @Schema(description = "Datos actualizados del insumo")
        SupplyResponse supply
) {
}

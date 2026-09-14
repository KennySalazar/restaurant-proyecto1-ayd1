package com.restaurante.web.dto.supply;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Respuesta de confirmación tras actualizar un insumo")
public record SupplyUpdateResponse(
                @Schema(description = "Mensaje de confirmación", example = "Insumo actualizado exitosamente") String message,

                @Schema(description = "Datos actualizados del insumo") SupplyResponse supply) {
}

package com.restaurante.web.dto.comanda;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Detalle de un platillo rechazado durante el envío de una comanda por falta de stock.
 */
@Schema(description = "Detalle de un platillo rechazado por stock insuficiente al enviar la comanda")
public record RejectedDishDetailResponse(
        @Schema(description = "Identificador del detalle de la comanda", example = "10")
        Long detailId,

        @Schema(description = "Nombre del platillo rechazado", example = "Hamburguesa Clásica")
        String dishName,

        @Schema(description = "Motivo del rechazo", example = "Stock insuficiente del insumo 'Carne de Res' (requerido: 2.0000, disponible: 0.0000)")
        String reason
) {
}

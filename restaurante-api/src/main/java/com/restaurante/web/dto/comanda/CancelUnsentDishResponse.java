package com.restaurante.web.dto.comanda;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Confirmación de eliminación de un platillo de una comanda antes del envío a cocina.
 */
@Schema(description = "Respuesta tras cancelar o eliminar un platillo en borrador antes del envío a cocina")
public record CancelUnsentDishResponse(
        @Schema(description = "Identificador único del detalle de comanda eliminado", example = "15")
        @JsonProperty("detalleId") @JsonAlias("detailId")
        Long detalleId,

        @Schema(description = "Nombre del platillo eliminado", example = "Hamburguesa Clásica")
        @JsonProperty("nombrePlatillo") @JsonAlias("dishName")
        String nombrePlatillo,

        @Schema(description = "Cantidad eliminada", example = "2")
        @JsonProperty("cantidad") @JsonAlias("quantity")
        short cantidad,

        @Schema(description = "Identificador de la comanda", example = "4")
        @JsonProperty("comandaId")
        Long comandaId,

        @Schema(description = "Identificador de la cuenta", example = "10")
        @JsonProperty("cuentaId") @JsonAlias("accountId")
        Long cuentaId,

        @Schema(description = "Número de ronda de la comanda", example = "1")
        @JsonProperty("numeroRonda") @JsonAlias("roundNumber")
        short numeroRonda,

        @Schema(description = "Indica si la comanda en borrador fue eliminada al quedar sin ítems", example = "false")
        @JsonProperty("comandaEliminada") @JsonAlias("comandaDeleted")
        boolean comandaEliminada,

        @Schema(description = "Cantidad de platillos restantes en la comanda", example = "1")
        @JsonProperty("platillosRestantesEnComanda") @JsonAlias("remainingDishesInComanda")
        int platillosRestantesEnComanda,

        @Schema(description = "Mensaje descriptivo del resultado", example = "Platillo eliminado exitosamente de la comanda antes del envío a cocina. El inventario no fue afectado.")
        @JsonProperty("mensaje") @JsonAlias("message")
        String mensaje
) {
}

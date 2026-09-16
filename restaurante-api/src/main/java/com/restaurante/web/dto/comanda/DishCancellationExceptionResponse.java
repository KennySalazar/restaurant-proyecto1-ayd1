package com.restaurante.web.dto.comanda;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * Respuesta tras registrar exitosamente una excepción de cancelación de un platillo.
 */
@Schema(description = "Respuesta de confirmación de excepción de cancelación de platillo")
public record DishCancellationExceptionResponse(
        @Schema(description = "Identificador único del registro de excepción de cancelación", example = "1")
        @JsonProperty("cancelacionId") @JsonAlias("cancellationId")
        Long cancelacionId,

        @Schema(description = "Identificador único del detalle de comanda", example = "15")
        @JsonProperty("detalleId") @JsonAlias("detailId")
        Long detalleId,

        @Schema(description = "Nombre registrado del platillo", example = "Hamburguesa Clásica")
        @JsonProperty("platilloNombre") @JsonAlias("dishName")
        String platilloNombre,

        @Schema(description = "Cantidad ordenada", example = "1")
        @JsonProperty("cantidad") @JsonAlias("quantity")
        short cantidad,

        @Schema(description = "Estado del platillo previo a la cancelación", example = "EN_PREPARACION")
        @JsonProperty("estadoAnterior") @JsonAlias("previousStatus")
        String estadoAnterior,

        @Schema(description = "Nuevo estado del platillo", example = "CANCELADO")
        @JsonProperty("estadoNuevo") @JsonAlias("newStatus")
        String estadoNuevo,

        @Schema(description = "Tipo de excepción", example = "CLIENTE")
        @JsonProperty("tipo") @JsonAlias("type")
        String tipo,

        @Schema(description = "Motivo de la cancelación", example = "Cliente cambió de opinión tras ordenar")
        @JsonProperty("motivo") @JsonAlias("reason")
        String motivo,

        @Schema(description = "Estado de la solicitud de excepción", example = "APROBADA")
        @JsonProperty("estadoSolicitud") @JsonAlias("requestStatus")
        String estadoSolicitud,

        @Schema(description = "Acción tomada sobre inventario", example = "REGISTRAR_MERMA")
        @JsonProperty("accionInventario") @JsonAlias("inventoryAction")
        String accionInventario,

        @Schema(description = "Identificador del usuario que solicitó o registró la cancelación", example = "5")
        @JsonProperty("solicitadaPorId") @JsonAlias("requestedById")
        Long solicitadaPorId,

        @Schema(description = "Nombre del usuario solicitante", example = "Juan Pérez")
        @JsonProperty("solicitadaPorNombre") @JsonAlias("requestedByName")
        String solicitadaPorNombre,

        @Schema(description = "Identificador del supervisor/administrador que autorizó la excepción", example = "1")
        @JsonProperty("autorizadaPorId") @JsonAlias("authorizedById")
        Long autorizadaPorId,

        @Schema(description = "Nombre del supervisor/administrador autorizador", example = "Admin General")
        @JsonProperty("autorizadaPorNombre") @JsonAlias("authorizedByName")
        String autorizadaPorNombre,

        @Schema(description = "Fecha y hora en que se registró la excepción")
        @JsonProperty("registradaEn") @JsonAlias("registeredAt")
        Instant registradaEn,

        @Schema(description = "Identificador de la comanda", example = "4")
        @JsonProperty("comandaId")
        Long comandaId,

        @Schema(description = "Identificador de la cuenta", example = "10")
        @JsonProperty("cuentaId") @JsonAlias("accountId")
        Long cuentaId,

        @Schema(description = "Número o identificador de mesa", example = "M-01")
        @JsonProperty("numeroMesa") @JsonAlias("tableNumber")
        String numeroMesa,

        @Schema(description = "Mensaje informativo de la operación", example = "Cancelación registrada como excepción exitosamente.")
        @JsonProperty("mensaje") @JsonAlias("message")
        String mensaje
) {
}

package com.restaurante.web.dto.comanda;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;

import java.util.List;

/**
 * Solicitud para agregar uno o varios platillos con sus modificadores y notas a una cuenta abierta.
 */
@Schema(description = "Solicitud para registrar platillos en una cuenta abierta")
public record AddDishToAccountRequest(
        @Schema(description = "Identificador del platillo (opcional si se utiliza la lista 'items')", example = "1")
        @JsonProperty("platilloId")
        @JsonAlias({"dishId", "platillo_id"})
        Long dishId,

        @Schema(description = "Cantidad ordenada del platillo (opcional si se utiliza la lista 'items')", example = "2")
        @JsonProperty("cantidad")
        @JsonAlias({"quantity", "cant"})
        Short quantity,

        @Schema(description = "Notas especiales de preparación para el platillo", example = "Término medio, sin cebolla")
        @JsonProperty("notas")
        @JsonAlias({"notes", "specialNotes", "notas_especiales"})
        String notes,

        @Schema(description = "Identificadores de los modificadores seleccionados", example = "[1, 2]")
        @JsonProperty("modificadoresIds")
        @JsonAlias({"modifierIds", "modificadores", "modificadores_ids"})
        List<Long> modifierIds,

        @Schema(description = "Listado opcional de platillos para agregar en lote")
        @JsonProperty("items")
        @JsonAlias({"platillos", "detalles"})
        @Valid
        List<AddDishItemRequest> items
) {
}

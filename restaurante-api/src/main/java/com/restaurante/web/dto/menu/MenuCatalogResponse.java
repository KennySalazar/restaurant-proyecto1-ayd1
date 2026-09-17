package com.restaurante.web.dto.menu;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Catálogo del menú operativo para el mesero: platillos activos con modificadores y combos disponibles.
 */
@Schema(description = "Catálogo del menú para operación, con platillos y combos disponibles")
public record MenuCatalogResponse(
        @Schema(description = "Platillos del catálogo con sus modificadores y disponibilidad", example = "[]")
        List<MenuDishResponse> dishes,

        @Schema(description = "Combos disponibles del catálogo", example = "[]")
        List<MenuComboResponse> combos
) {
}
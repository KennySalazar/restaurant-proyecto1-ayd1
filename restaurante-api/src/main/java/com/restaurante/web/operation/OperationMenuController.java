package com.restaurante.web.operation;

import com.restaurante.application.operation.OperationMenuService;
import com.restaurante.web.dto.menu.MenuCatalogResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador de operación para consultar el menú del mesero.
 */
@RestController
@RequestMapping("/operacion/menu")
@Tag(
        name = "Menú (Operación)",
        description = "Consulta del menú operativo para armar comandas"
)
@SecurityRequirement(name = "bearerAuth")
public class OperationMenuController {

    private final OperationMenuService operationMenuService;

    public OperationMenuController(OperationMenuService operationMenuService) {
        this.operationMenuService = operationMenuService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('WAITER', 'ADMIN')")
    @Operation(
            summary = "Consultar el menú operativo",
            description = "Retorna los platillos activos del catálogo con su disponibilidad actual y modificadores disponibles, junto con los combos vigentes, para que el mesero arme la comanda."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Menú operativo obtenido exitosamente",
                    content = @Content(schema = @Schema(implementation = MenuCatalogResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "No autenticado o token no provisto",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Acceso denegado (requiere rol de mesero o administrador)",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    public ResponseEntity<MenuCatalogResponse> getMenu() {
        return ResponseEntity.ok(operationMenuService.getActiveMenu());
    }
}
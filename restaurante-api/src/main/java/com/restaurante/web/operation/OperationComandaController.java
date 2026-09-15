package com.restaurante.web.operation;

import com.restaurante.application.inventory.ComandaInventoryService;
import com.restaurante.web.dto.comanda.ComandaInventoryProcessResponse;
import com.restaurante.web.dto.comanda.ComandaResponse;
import com.restaurante.web.dto.comanda.CreateComandaRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador para la gestión operativa de comandas y descuento de inventario.
 */
@RestController
@RequestMapping("/operacion/comandas")
@Tag(name = "Comandas (Operación)", description = "Operaciones de registro, envío y descuento de inventario por comandas")
@SecurityRequirement(name = "bearerAuth")
public class OperationComandaController {

    private final ComandaInventoryService comandaInventoryService;

    public OperationComandaController(ComandaInventoryService comandaInventoryService) {
        this.comandaInventoryService = comandaInventoryService;
    }

    /**
     * Registra una comanda para una cuenta/mesa. Opcionalmente puede enviarse de inmediato.
     */
    @PostMapping
    @Operation(summary = "Registrar comanda de operación")
    public ResponseEntity<ComandaInventoryProcessResponse> createComanda(
            @Valid @RequestBody CreateComandaRequest request,
            Authentication authentication) {

        ComandaInventoryProcessResponse response = comandaInventoryService.createComanda(request, authentication);
        HttpStatus status = Boolean.TRUE.equals(request.sendImmediately()) ? HttpStatus.OK : HttpStatus.CREATED;
        return ResponseEntity.status(status).body(response);
    }

    /**
     * Envía una comanda registrada en borrador a cocina y descuenta automáticamente los insumos en el kardex.
     */
    @PostMapping("/{id}/enviar")
    @PreAuthorize("hasAnyRole('WAITER', 'ADMIN')")
    @Operation(
            summary = "Enviar comanda y descontar insumos automáticamente",
            description = "Valida y descuenta automáticamente los insumos de inventario de cada platillo y modificador. Notifica a cocina en tiempo real y marca los platillos como recibidos. Si algún platillo no tiene stock suficiente, rechaza su envío, lo marca como no disponible y notifica al mesero."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Comanda enviada a cocina exitosamente",
                    content = @Content(schema = @Schema(implementation = ComandaInventoryProcessResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Comanda vacía, sin platillos en borrador o stock insuficiente de insumos",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Comanda no encontrada",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "La comanda ya fue procesada previamente",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    public ResponseEntity<ComandaInventoryProcessResponse> sendComanda(
            @PathVariable Long id,
            Authentication authentication) {

        ComandaInventoryProcessResponse response = comandaInventoryService.sendComanda(id, authentication);
        return ResponseEntity.ok(response);
    }

    /**
     * Consulta una comanda por su identificador.
     */
    @GetMapping("/{id}")
    @Operation(summary = "Consultar comanda por identificador")
    public ResponseEntity<ComandaResponse> getComandaById(@PathVariable Long id) {
        ComandaResponse response = comandaInventoryService.getComandaById(id);
        return ResponseEntity.ok(response);
    }
}

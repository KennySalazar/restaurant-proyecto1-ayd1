package com.restaurante.web.operation;

import com.restaurante.application.table.TableSeatingService;
import com.restaurante.application.table.TableService;
import com.restaurante.domain.model.TableStatus;
import com.restaurante.web.dto.table.SeatReservationRequest;
import com.restaurante.web.dto.table.SeatReservationResponse;
import com.restaurante.web.dto.table.TableResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controlador de operación para la visualización y consulta del estado de las mesas del salón.
 */
@RestController
@RequestMapping("/operacion/mesas")
@Tag(
        name = "Mesas (Operación)",
        description = "Consulta operativa del estado actual de las mesas del salón y asignación para meseros"
)
@SecurityRequirement(name = "bearerAuth")
public class TableOperationController {

    private final TableService tableService;
    private final TableSeatingService tableSeatingService;

    public TableOperationController(TableService tableService, TableSeatingService tableSeatingService) {
        this.tableService = tableService;
        this.tableSeatingService = tableSeatingService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('WAITER', 'ADMIN')")
    @Operation(
            summary = "Consultar el estado de las mesas del salón",
            description = "Permite al mesero visualizar el estado actual de cada mesa (LIBRE, RESERVADA, OCUPADA, CUENTA_SOLICITADA) con su número, capacidad y zona asignada. Permite filtrar opcionalmente por zona o por estado de mesa."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Listado de mesas del salón obtenido exitosamente",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = TableResponse.class)))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "No autenticado o token JWT no provisto",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Acceso denegado (requiere rol de mesero o administrador)",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    public ResponseEntity<List<TableResponse>> getTables(
            @Parameter(description = "Identificador opcional de la zona para filtrar")
            @RequestParam(required = false) Long zonaId,
            @Parameter(description = "Estado opcional de la mesa para filtrar (LIBRE, RESERVADA, OCUPADA, CUENTA_SOLICITADA)")
            @RequestParam(required = false) TableStatus estado,
            Authentication authentication) {

        return ResponseEntity.ok(tableService.getTables(zonaId, estado, authentication));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('WAITER', 'ADMIN')")
    @Operation(
            summary = "Consultar el detalle y estado de una mesa específica",
            description = "Retorna la información y estado actual de una mesa en particular para el mesero."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Mesa obtenida exitosamente",
                    content = @Content(schema = @Schema(implementation = TableResponse.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "No autenticado o token JWT no provisto",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Acceso denegado (requiere rol de mesero o administrador)",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Mesa no encontrada",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    public ResponseEntity<TableResponse> getTable(
            @Parameter(description = "Identificador único de la mesa", required = true)
            @PathVariable Long id,
            Authentication authentication) {

        return ResponseEntity.ok(tableService.getTable(id, authentication));
    }

    @PostMapping({ "/{id}/sentar-reserva", "/{id}/sentar" })
    @PreAuthorize("hasAnyRole('WAITER', 'ADMIN')")
    @Operation(
            summary = "Sentar cliente con reserva en la mesa",
            description = "Registra la llegada y sienta al cliente de una reserva activa en su mesa asignada, cambiando el estado de la mesa de 'RESERVADA' a 'OCUPADA' y abriendo la cuenta correspondiente. Si la mesa está reservada para otro horario o cliente distinto, la acción es bloqueada."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Cliente sentado exitosamente y mesa cambiada a ocupada",
                    content = @Content(schema = @Schema(implementation = SeatReservationResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Datos inválidos o no existe reserva activa",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "No autenticado o token JWT no provisto",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Acceso denegado (requiere rol de mesero o administrador)",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Mesa o reserva no encontrada",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "La mesa está reservada para otro horario/cliente, o ya se encuentra ocupada",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    public ResponseEntity<SeatReservationResponse> seatReservation(
            @Parameter(description = "Identificador único de la mesa", required = true)
            @PathVariable Long id,
            @Valid @RequestBody(required = false) SeatReservationRequest request,
            Authentication authentication) {

        return ResponseEntity.ok(tableSeatingService.seatReservation(id, request, authentication));
    }
}
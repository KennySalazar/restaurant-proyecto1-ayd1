package com.restaurante.web.operation;

import com.restaurante.application.table.TableSeatingService;
import com.restaurante.application.waitlist.WaitlistService;
import com.restaurante.web.dto.table.SeatWaitlistRequest;
import com.restaurante.web.dto.table.SeatWaitlistResponse;
import com.restaurante.web.dto.waitlist.WaitlistQueueEntryResponse;
import com.restaurante.web.dto.waitlist.WaitlistSuggestionResponse;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controlador de operación para la consulta de lista de espera y asignación de mesas a clientes sugeridos para meseros.
 */
@RestController
@RequestMapping("/operacion/lista-espera")
@Tag(
        name = "Lista de espera (Operación)",
        description = "Consulta de la cola de espera y asignación de mesas a clientes sugeridos por el sistema"
)
@SecurityRequirement(name = "bearerAuth")
public class OperationWaitlistController {

    private final WaitlistService waitlistService;
    private final TableSeatingService tableSeatingService;

    public OperationWaitlistController(
            WaitlistService waitlistService,
            TableSeatingService tableSeatingService) {
        this.waitlistService = waitlistService;
        this.tableSeatingService = tableSeatingService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('WAITER', 'ADMIN')")
    @Operation(
            summary = "Consultar la cola activa de la lista de espera",
            description = "Devuelve los clientes actualmente en espera o con sugerencia activa ordenados por orden de llegada."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Listado de clientes en cola de espera",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = WaitlistQueueEntryResponse.class)))
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
    public ResponseEntity<List<WaitlistQueueEntryResponse>> getWaitlist(Authentication authentication) {
        return ResponseEntity.ok(waitlistService.getWaitlist(authentication));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('WAITER', 'ADMIN')")
    @Operation(
            summary = "Consultar detalle de un cliente en lista de espera",
            description = "Devuelve la información de un cliente específico dentro de la cola activa de espera."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Información del cliente en lista de espera",
                    content = @Content(schema = @Schema(implementation = WaitlistQueueEntryResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Cliente no encontrado en la lista de espera",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    public ResponseEntity<WaitlistQueueEntryResponse> getWaitlistEntry(
            @Parameter(description = "Identificador del cliente en lista de espera", required = true)
            @PathVariable Long id,
            Authentication authentication) {
        return ResponseEntity.ok(waitlistService.getWaitlistEntry(id, authentication));
    }

    @PostMapping("/sugerencias/mesa/{mesaId}")
    @PreAuthorize("hasAnyRole('WAITER', 'ADMIN')")
    @Operation(
            summary = "Sugerir el primer cliente compatible para una mesa liberada",
            description = "Evalúa la lista de espera y sugiere al primer cliente en turno cuyo grupo quepa en la mesa liberada."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Cliente compatible sugerido para la mesa",
                    content = @Content(schema = @Schema(implementation = WaitlistSuggestionResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Mesa o cliente no encontrado",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "La mesa no está disponible o no hay sugerencia compatible",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    public ResponseEntity<WaitlistSuggestionResponse> suggestNextCompatible(
            @Parameter(description = "Identificador de la mesa liberada", required = true)
            @PathVariable Long mesaId,
            Authentication authentication) {
        return ResponseEntity.ok(waitlistService.suggestNextCompatible(mesaId, authentication));
    }

    @PostMapping({ "/{id}/sentar", "/{id}/confirmar-sugerencia" })
    @PreAuthorize("hasAnyRole('WAITER', 'ADMIN')")
    @Operation(
            summary = "Sentar al cliente sugerido de la lista de espera",
            description = "Confirma sentar al cliente sugerido por el sistema en su mesa asignada: la mesa cambia a estado 'OCUPADA', el cliente sale de la lista de espera (estado SENTADA) y se abre la cuenta de consumo."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Cliente sentado exitosamente y mesa ocupada",
                    content = @Content(schema = @Schema(implementation = SeatWaitlistResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Datos inválidos",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Entrada de espera o mesa no encontrada",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "El cliente no tiene sugerencia activa, la mesa está ocupada o bloqueada por reserva",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    public ResponseEntity<SeatWaitlistResponse> seatCustomer(
            @Parameter(description = "Identificador de la entrada en lista de espera", required = true)
            @PathVariable Long id,
            @Valid @RequestBody(required = false) SeatWaitlistRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(tableSeatingService.seatWaitlistEntryById(id, request, authentication));
    }

    @PutMapping("/{id}/confirmar-sugerencia")
    @PreAuthorize("hasAnyRole('WAITER', 'ADMIN')")
    @Operation(
            summary = "Confirmar sugerencia y sentar al cliente (alias PUT)",
            description = "Alias PUT para compatibilidad: confirma la asignación de mesa sugerida y sienta al cliente."
    )
    public ResponseEntity<SeatWaitlistResponse> confirmSuggestionPut(
            @Parameter(description = "Identificador de la entrada en lista de espera", required = true)
            @PathVariable Long id,
            @Valid @RequestBody(required = false) SeatWaitlistRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(tableSeatingService.seatWaitlistEntryById(id, request, authentication));
    }

    @PutMapping("/{id}/rechazar-sugerencia")
    @PreAuthorize("hasAnyRole('WAITER', 'ADMIN')")
    @Operation(
            summary = "Rechazar la sugerencia de mesa",
            description = "Rechaza la sugerencia de mesa actual y devuelve al cliente al estado de espera en la cola."
    )
    public ResponseEntity<WaitlistQueueEntryResponse> rejectSuggestion(
            @Parameter(description = "Identificador de la entrada en lista de espera", required = true)
            @PathVariable Long id,
            Authentication authentication) {
        return ResponseEntity.ok(waitlistService.rejectSuggestion(id, authentication));
    }
}

package com.restaurante.web.operation;

import com.restaurante.application.reservation.ReservationService;
import com.restaurante.application.table.TableSeatingService;
import com.restaurante.web.dto.reservation.ReservationResponse;
import com.restaurante.web.dto.table.SeatReservationRequest;
import com.restaurante.web.dto.table.SeatReservationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controlador de operación para la llegada y asignación de mesas a clientes con reserva.
 */
@RestController
@RequestMapping("/operacion/reservas")
@Tag(
        name = "Reservas (Operación)",
        description = "Operaciones para el registro de llegada y asignación de mesas a clientes con reserva"
)
@SecurityRequirement(name = "bearerAuth")
public class OperationReservationController {

    private final TableSeatingService tableSeatingService;
    private final ReservationService reservationService;

    public OperationReservationController(TableSeatingService tableSeatingService,
                                          ReservationService reservationService) {
        this.tableSeatingService = tableSeatingService;
        this.reservationService = reservationService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('WAITER', 'ADMIN')")
    @Operation(
            summary = "Consultar las reservas próximas",
            description = "Retorna las reservaciones pendientes o confirmadas futuras del restaurante, para que el mesero prepare la mesa y registre la llegada del cliente."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Reservas próximas obtenidas exitosamente",
                    content = @Content(schema = @Schema(implementation = ReservationResponse.class))
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
    public List<ReservationResponse> getUpcomingReservations(Authentication authentication) {
        return reservationService.getUpcomingReservations(authentication);
    }

    @PostMapping({ "/{id}/sentar", "/{id}/llegada" })
    @PreAuthorize("hasAnyRole('WAITER', 'ADMIN')")
    @Operation(
            summary = "Registrar llegada y sentar cliente de una reserva",
            description = "Registra que el cliente de la reserva ha llegado y lo sienta en la mesa asignada, cambiando su estado a 'OCUPADA' y abriendo la cuenta correspondiente."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Cliente de la reserva sentado exitosamente y mesa cambiada a ocupada",
                    content = @Content(schema = @Schema(implementation = SeatReservationResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Datos inválidos o reserva inactiva",
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
                    description = "Reserva o mesa no encontrada",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "La mesa está reservada para otro horario/cliente o ya se encuentra ocupada",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))
            )
    })
    public ResponseEntity<SeatReservationResponse> seatReservation(
            @Parameter(description = "Identificador único de la reserva", required = true)
            @PathVariable Long id,
            @Valid @RequestBody(required = false) SeatReservationRequest request,
            Authentication authentication) {

        return ResponseEntity.ok(tableSeatingService.seatReservationByReservationId(id, request, authentication));
    }
}

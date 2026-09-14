package com.restaurante.web.admin;

import com.restaurante.application.reservation.ReservationService;
import com.restaurante.web.dto.reservation.CreateReservationRequest;
import com.restaurante.web.dto.reservation.ReservationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/reservas")
@Tag(
        name = "Reservas",
        description = "Gestión administrativa de reservas"
)
@SecurityRequirement(name = "bearerAuth")
public class ReservationAdminController {

    private final ReservationService reservationService;

    public ReservationAdminController(
            ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registrar una reserva de mesa")
    public ReservationResponse createReservation(
            @Valid @RequestBody CreateReservationRequest request,
            Authentication authentication) {

        return reservationService.createReservation(
                request,
                authentication
        );
    }
}
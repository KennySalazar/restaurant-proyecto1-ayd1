package com.restaurante.application.reservation;

import com.restaurante.domain.model.Reservation;
import com.restaurante.domain.model.RestaurantConfiguration;
import com.restaurante.domain.model.RestaurantConfigurationStatus;
import com.restaurante.domain.model.RestaurantTable;
import com.restaurante.domain.model.RestaurantUserProfile;
import com.restaurante.domain.repository.ReservationRepository;
import com.restaurante.domain.repository.RestaurantConfigurationRepository;
import com.restaurante.domain.repository.RestaurantTableRepository;
import com.restaurante.domain.repository.RestaurantUserProfileRepository;
import com.restaurante.exception.ApiException;
import com.restaurante.security.JwtData;
import com.restaurante.web.dto.reservation.CreateReservationRequest;
import com.restaurante.web.dto.reservation.ReservationResponse;
import com.restaurante.web.dto.reservation.ReservationTableResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.UUID;

@Service
public class ReservationService {

    private static final ZoneId GUATEMALA =
            ZoneId.of("America/Guatemala");

    private final ReservationRepository reservations;
    private final RestaurantTableRepository tables;
    private final RestaurantUserProfileRepository profiles;
    private final RestaurantConfigurationRepository configurations;

    public ReservationService(
            ReservationRepository reservations,
            RestaurantTableRepository tables,
            RestaurantUserProfileRepository profiles,
            RestaurantConfigurationRepository configurations) {

        this.reservations = reservations;
        this.tables = tables;
        this.profiles = profiles;
        this.configurations = configurations;
    }

    @Transactional
    public ReservationResponse createReservation(
            CreateReservationRequest request,
            Authentication authentication) {

        AuthenticatedRestaurant context =
                getAuthenticatedRestaurant(authentication);

        OffsetDateTime now =
                OffsetDateTime.now(GUATEMALA);

        OffsetDateTime startDateTime =
                request.fechaHoraInicio()
                        .atZoneSameInstant(GUATEMALA)
                        .toOffsetDateTime();

        if (!startDateTime.isAfter(now)) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "reservation_must_be_future",
                    "Horario de reserva inválido",
                    "La reserva debe programarse para un horario futuro"
            );
        }

        RestaurantTable table = tables
                .findByIdAndRestaurantId(
                        request.mesaId(),
                        context.restaurantId()
                )
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "table_not_found",
                        "Mesa no encontrada",
                        "La mesa seleccionada no existe"
                ));

        if (!table.isActive()) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "table_inactive",
                    "Mesa retirada",
                    "Solamente pueden reservarse mesas activas"
            );
        }

        if (request.cantidadPersonas() > table.getCapacity()) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "insufficient_table_capacity",
                    "Capacidad insuficiente",
                    "La capacidad de la mesa es insuficiente para la reserva"
            );
        }

        RestaurantConfiguration configuration = configurations
                .findByRestaurantIdAndStatus(
                        context.restaurantId(),
                        RestaurantConfigurationStatus.VIGENTE
                )
                .orElseThrow(() -> new ApiException(
                        HttpStatus.CONFLICT,
                        "restaurant_configuration_not_found",
                        "Configuración no encontrada",
                        "El restaurante no tiene una configuración vigente"
                ));

        OffsetDateTime endDateTime =
                startDateTime.plusMinutes(
                        configuration.getReservationDurationMinutes()
                );

        if (reservations.hasScheduleConflict(
                table.getId(),
                startDateTime,
                endDateTime)) {

            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "table_not_available",
                    "Mesa no disponible",
                    "La mesa ya tiene una reserva en el horario solicitado"
            );
        }

        String reservationCode =
                generateReservationCode(context.restaurantId());

        String notes = request.notas();

        if (notes != null) {
            notes = notes.trim();

            if (notes.isEmpty()) {
                notes = null;
            }
        }

        Reservation reservation = new Reservation(
                context.restaurantId(),
                table,
                reservationCode,
                request.nombreCliente().trim(),
                request.telefonoCliente().trim(),
                request.cantidadPersonas(),
                startDateTime,
                endDateTime,
                notes,
                context.userId()
        );

        try {
            Reservation saved =
                    reservations.saveAndFlush(reservation);

            return toResponse(saved);

        } catch (DataIntegrityViolationException exception) {

            String message =
                    exception.getMostSpecificCause().getMessage();

            if (message != null
                    && message.contains(
                    "ex_reservas_mesa_horario")) {

                throw new ApiException(
                        HttpStatus.CONFLICT,
                        "table_not_available",
                        "Mesa no disponible",
                        "La mesa ya tiene una reserva en el horario solicitado"
                );
            }

            throw exception;
        }
    }

    private String generateReservationCode(
            Long restaurantId) {

        String code;

        do {
            code = "RES-"
                    + UUID.randomUUID()
                    .toString()
                    .substring(0, 8)
                    .toUpperCase();

        } while (reservations
                .existsByRestaurantIdAndReservationCode(
                        restaurantId,
                        code
                ));

        return code;
    }

    private AuthenticatedRestaurant getAuthenticatedRestaurant(
            Authentication authentication) {

        if (authentication == null
                || !(authentication.getDetails()
                instanceof JwtData jwtData)) {

            throw new ApiException(
                    HttpStatus.UNAUTHORIZED,
                    "invalid_authenticated_user",
                    "Usuario no autenticado",
                    "No fue posible identificar al usuario autenticado"
            );
        }

        RestaurantUserProfile profile = profiles
                .findById(jwtData.userId())
                .orElseThrow(() -> new ApiException(
                        HttpStatus.CONFLICT,
                        "restaurant_profile_not_found",
                        "Perfil de restaurante no encontrado",
                        "El usuario autenticado no tiene un perfil asociado al restaurante"
                ));

        return new AuthenticatedRestaurant(
                jwtData.userId(),
                profile.getRestaurantId()
        );
    }

    private ReservationResponse toResponse(
            Reservation reservation) {

        RestaurantTable table =
                reservation.getTable();

        OffsetDateTime start =
                reservation.getStartDateTime()
                        .atZoneSameInstant(GUATEMALA)
                        .toOffsetDateTime();

        OffsetDateTime end =
                reservation.getEndDateTime()
                        .atZoneSameInstant(GUATEMALA)
                        .toOffsetDateTime();

        return new ReservationResponse(
                reservation.getId(),
                reservation.getReservationCode(),
                reservation.getCustomerName(),
                reservation.getCustomerPhone(),
                reservation.getPeopleCount(),
                start,
                end,
                reservation.getStatus(),
                reservation.getNotes(),
                new ReservationTableResponse(
                        table.getId(),
                        table.getNumber(),
                        table.getCapacity(),
                        table.getZone().getName()
                )
        );
    }

    private record AuthenticatedRestaurant(
            Long userId,
            Long restaurantId
    ) {
    }
}
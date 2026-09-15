package com.restaurante.application.table;

import com.restaurante.domain.model.Account;
import com.restaurante.domain.model.Reservation;
import com.restaurante.domain.model.ReservationStatus;
import com.restaurante.domain.model.RestaurantTable;
import com.restaurante.domain.model.RestaurantUserProfile;
import com.restaurante.domain.model.TableStatus;
import com.restaurante.domain.repository.AccountRepository;
import com.restaurante.domain.repository.ReservationRepository;
import com.restaurante.domain.repository.RestaurantTableRepository;
import com.restaurante.domain.repository.RestaurantUserProfileRepository;
import com.restaurante.exception.ApiException;
import com.restaurante.security.JwtData;
import com.restaurante.web.dto.table.SeatReservationRequest;
import com.restaurante.web.dto.table.SeatReservationResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

/**
 * Servicio de aplicación para sentar clientes con reserva en las mesas del salón.
 */
@Service
public class TableSeatingService {

    private static final ZoneId GUATEMALA = ZoneId.of("America/Guatemala");

    private final RestaurantTableRepository tableRepository;
    private final ReservationRepository reservationRepository;
    private final AccountRepository accountRepository;
    private final RestaurantUserProfileRepository userProfileRepository;

    public TableSeatingService(
            RestaurantTableRepository tableRepository,
            ReservationRepository reservationRepository,
            AccountRepository accountRepository,
            RestaurantUserProfileRepository userProfileRepository) {
        this.tableRepository = tableRepository;
        this.reservationRepository = reservationRepository;
        this.accountRepository = accountRepository;
        this.userProfileRepository = userProfileRepository;
    }

    /**
     * Sienta a un cliente con reserva vigente en una mesa específica del restaurante.
     * Valida que la reserva corresponda al horario y mesa indicada; en caso contrario,
     * impide la acción e indica que la mesa está reservada para otro horario/cliente.
     */
    @Transactional
    public SeatReservationResponse seatReservation(
            Long tableId,
            SeatReservationRequest request,
            Authentication authentication) {

        AuthenticatedUser context = getAuthenticatedUser(authentication);
        Long restaurantId = context.restaurantId();

        RestaurantTable table = tableRepository
                .findByIdAndRestaurantId(tableId, restaurantId)
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
                    "Mesa inactiva",
                    "La mesa seleccionada ya no se encuentra activa"
            );
        }

        if (table.getStatus() == TableStatus.OCUPADA) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "table_already_occupied",
                    "Mesa ocupada",
                    "La mesa ya se encuentra ocupada"
            );
        }

        if (table.getStatus() == TableStatus.CUENTA_SOLICITADA) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "table_payment_requested",
                    "Cuenta en cobro",
                    "La mesa tiene una cuenta en proceso de cobro"
            );
        }

        OffsetDateTime now = OffsetDateTime.now(GUATEMALA);

        // Buscar reservas activas en la mesa alrededor del horario actual
        List<Reservation> activeReservationsOnTable = reservationRepository
                .findActiveReservationsForTable(table.getId(), now.minusMinutes(45), now.plusMinutes(15));

        Reservation targetReservation;

        if (request != null && request.reservaId() != null) {
            Reservation res = reservationRepository
                    .findByIdAndRestaurantId(request.reservaId(), restaurantId)
                    .orElseThrow(() -> new ApiException(
                            HttpStatus.NOT_FOUND,
                            "reservation_not_found",
                            "Reserva no encontrada",
                            "La reserva seleccionada no existe"
                    ));

            if (!res.getTable().getId().equals(table.getId())) {
                throw new ApiException(
                        HttpStatus.CONFLICT,
                        "table_reserved_for_another_client",
                        "Mesa no coincidente",
                        "La mesa está reservada para otro horario/cliente"
                );
            }

            if (res.getStatus() == ReservationStatus.CANCELADA || res.getStatus() == ReservationStatus.NO_ASISTIO) {
                throw new ApiException(
                        HttpStatus.BAD_REQUEST,
                        "invalid_reservation_status",
                        "Reserva no activa",
                        "La reserva seleccionada se encuentra cancelada o vencida"
                );
            }

            targetReservation = res;

        } else if (request != null && request.codigoReserva() != null && !request.codigoReserva().trim().isEmpty()) {
            Reservation res = reservationRepository
                    .findByRestaurantIdAndReservationCode(restaurantId, request.codigoReserva().trim().toUpperCase())
                    .orElseThrow(() -> new ApiException(
                            HttpStatus.NOT_FOUND,
                            "reservation_not_found",
                            "Reserva no encontrada",
                            "No se encontró una reserva con el código indicado"
                    ));

            if (!res.getTable().getId().equals(table.getId())) {
                throw new ApiException(
                        HttpStatus.CONFLICT,
                        "table_reserved_for_another_client",
                        "Mesa no coincidente",
                        "La mesa está reservada para otro horario/cliente"
                );
            }

            if (res.getStatus() == ReservationStatus.CANCELADA || res.getStatus() == ReservationStatus.NO_ASISTIO) {
                throw new ApiException(
                        HttpStatus.BAD_REQUEST,
                        "invalid_reservation_status",
                        "Reserva no activa",
                        "La reserva seleccionada se encuentra cancelada o vencida"
                );
            }

            targetReservation = res;

        } else if (!activeReservationsOnTable.isEmpty()) {
            Reservation res = activeReservationsOnTable.get(0);

            if (request != null && request.nombreCliente() != null && !request.nombreCliente().trim().isEmpty()) {
                if (!res.getCustomerName().trim().equalsIgnoreCase(request.nombreCliente().trim())) {
                    throw new ApiException(
                            HttpStatus.CONFLICT,
                            "table_reserved_for_another_client",
                            "Cliente no coincide",
                            "La mesa está reservada para otro horario/cliente"
                    );
                }
            }

            targetReservation = res;

        } else {
            if (table.getStatus() == TableStatus.RESERVADA) {
                throw new ApiException(
                        HttpStatus.CONFLICT,
                        "table_reserved_for_another_client",
                        "Mesa reservada",
                        "La mesa está reservada para otro horario/cliente"
                );
            }

            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "no_active_reservation",
                    "Sin reserva activa",
                    "No existe una reserva activa para esta mesa en el horario actual"
            );
        }

        // Si la mesa está reservada para otra reserva activa y se intenta sentar una reserva distinta
        if (!activeReservationsOnTable.isEmpty()) {
            Reservation currentActiveOnTable = activeReservationsOnTable.get(0);
            if (!currentActiveOnTable.getId().equals(targetReservation.getId())) {
                throw new ApiException(
                        HttpStatus.CONFLICT,
                        "table_reserved_for_another_client",
                        "Mesa reservada",
                        "La mesa está reservada para otro horario/cliente"
                );
            }
        }

        // Si la reserva no corresponde al horario vigente (ej. horas antes o después de la reserva)
        if (now.isBefore(targetReservation.getStartDateTime().minusMinutes(60)) || now.isAfter(targetReservation.getEndDateTime())) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "table_reserved_for_another_client",
                    "Horario no vigente",
                    "La mesa está reservada para otro horario/cliente"
            );
        }

        // Actualizar estado de la mesa de 'RESERVADA' a 'OCUPADA'
        table.occupy();
        tableRepository.save(table);

        // Actualizar estado de la reserva
        targetReservation.markClientPresent();
        reservationRepository.save(targetReservation);

        // Abrir o asociar cuenta activa en la mesa
        short peopleCount = (request != null && request.cantidadPersonas() != null && request.cantidadPersonas() > 0)
                ? request.cantidadPersonas()
                : (targetReservation.getPeopleCount() != null ? targetReservation.getPeopleCount() : (short) 2);

        Optional<Account> existingAccountOpt = accountRepository
                .findActiveByTableIdAndRestaurantId(table.getId(), restaurantId);

        Account account;
        if (existingAccountOpt.isPresent()) {
            account = existingAccountOpt.get();
            if (account.getReservationId() == null) {
                account.setReservationId(targetReservation.getId());
                accountRepository.save(account);
            }
        } else {
            String accountNumber = generateAccountNumber(restaurantId, table.getNumber());
            account = new Account(restaurantId, table.getId(), context.userId(), accountNumber, peopleCount);
            account.setReservationId(targetReservation.getId());
            if (request != null && request.notas() != null && !request.notas().trim().isEmpty()) {
                account.setNotes(request.notas().trim());
            }
            account = accountRepository.save(account);
        }

        return new SeatReservationResponse(
                "Cliente de la reserva sentado exitosamente. La mesa cambió a estado ocupada.",
                table.getId(),
                table.getNumber(),
                TableStatus.OCUPADA,
                targetReservation.getId(),
                targetReservation.getReservationCode(),
                targetReservation.getCustomerName(),
                peopleCount,
                account.getId(),
                account.getAccountNumber(),
                Instant.now()
        );
    }

    /**
     * Sienta a un cliente identificándolo directamente por su ID de reserva.
     */
    @Transactional
    public SeatReservationResponse seatReservationByReservationId(
            Long reservationId,
            SeatReservationRequest request,
            Authentication authentication) {

        AuthenticatedUser context = getAuthenticatedUser(authentication);
        Reservation reservation = reservationRepository
                .findByIdAndRestaurantId(reservationId, context.restaurantId())
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "reservation_not_found",
                        "Reserva no encontrada",
                        "La reserva seleccionada no existe"
                ));

        SeatReservationRequest effectiveRequest = new SeatReservationRequest(
                reservation.getId(),
                reservation.getReservationCode(),
                request != null ? request.nombreCliente() : reservation.getCustomerName(),
                request != null && request.cantidadPersonas() != null ? request.cantidadPersonas() : reservation.getPeopleCount(),
                request != null ? request.notas() : null
        );

        return seatReservation(reservation.getTable().getId(), effectiveRequest, authentication);
    }

    private String generateAccountNumber(Long restaurantId, String tableNumber) {
        String base = "CTA-" + tableNumber.trim() + "-";
        int suffix = (int) (System.currentTimeMillis() % 100000);
        String candidate = base + suffix;
        while (accountRepository.existsByRestaurantIdAndAccountNumber(restaurantId, candidate)) {
            suffix++;
            candidate = base + suffix;
        }
        return candidate;
    }

    private AuthenticatedUser getAuthenticatedUser(Authentication authentication) {
        if (authentication == null || !(authentication.getDetails() instanceof JwtData jwtData)) {
            throw new ApiException(
                    HttpStatus.UNAUTHORIZED,
                    "invalid_authenticated_user",
                    "Usuario no autenticado",
                    "No fue posible identificar al usuario autenticado"
            );
        }

        RestaurantUserProfile profile = userProfileRepository
                .findById(jwtData.userId())
                .orElseThrow(() -> new ApiException(
                        HttpStatus.CONFLICT,
                        "restaurant_profile_not_found",
                        "Perfil de restaurante no encontrado",
                        "El usuario autenticado no tiene un perfil asociado al restaurante"
                ));

        return new AuthenticatedUser(jwtData.userId(), profile.getRestaurantId());
    }

    private record AuthenticatedUser(Long userId, Long restaurantId) {
    }
}

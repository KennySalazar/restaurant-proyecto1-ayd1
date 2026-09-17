package com.restaurante.application.table;

import com.restaurante.domain.model.Account;
import com.restaurante.domain.model.Reservation;
import com.restaurante.domain.model.ReservationStatus;
import com.restaurante.domain.model.RestaurantTable;
import com.restaurante.domain.model.RestaurantUserProfile;
import com.restaurante.domain.model.TableStatus;
import com.restaurante.domain.model.WaitlistEntry;
import com.restaurante.domain.model.WaitlistStatus;
import com.restaurante.domain.repository.AccountRepository;
import com.restaurante.domain.repository.ReservationRepository;
import com.restaurante.domain.repository.RestaurantTableRepository;
import com.restaurante.domain.repository.RestaurantUserProfileRepository;
import com.restaurante.domain.repository.WaitlistRepository;
import com.restaurante.exception.ApiException;
import com.restaurante.security.JwtData;
import com.restaurante.web.dto.table.SeatReservationRequest;
import com.restaurante.web.dto.table.SeatReservationResponse;
import com.restaurante.web.dto.table.SeatWaitlistRequest;
import com.restaurante.web.dto.table.SeatWaitlistResponse;
import com.restaurante.web.dto.waitlist.WaitlistSuggestionResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 * Servicio de aplicación para sentar clientes (con reserva o desde lista de espera) en las mesas del salón.
 */
@Service
public class TableSeatingService {

    private static final ZoneId GUATEMALA = ZoneId.of("America/Guatemala");

    private static final Logger log = LoggerFactory.getLogger(TableSeatingService.class);

    private final RestaurantTableRepository tableRepository;
    private final ReservationRepository reservationRepository;
    private final AccountRepository accountRepository;
    private final RestaurantUserProfileRepository userProfileRepository;
    private final WaitlistRepository waitlistRepository;

    public TableSeatingService(
            RestaurantTableRepository tableRepository,
            ReservationRepository reservationRepository,
            AccountRepository accountRepository,
            RestaurantUserProfileRepository userProfileRepository,
            WaitlistRepository waitlistRepository) {
        this.tableRepository = tableRepository;
        this.reservationRepository = reservationRepository;
        this.accountRepository = accountRepository;
        this.userProfileRepository = userProfileRepository;
        this.waitlistRepository = waitlistRepository;
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

        try {
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

        // Actualizar estado de la reserva: si está PENDIENTE, confirmar primero y flush
        if (targetReservation.getStatus() == ReservationStatus.PENDIENTE) {
            targetReservation.setStatus(ReservationStatus.CONFIRMADA);
            reservationRepository.save(targetReservation);
            reservationRepository.flush();
        }
        targetReservation.markClientPresent();
        reservationRepository.save(targetReservation);
        reservationRepository.flush(); // forzar flush para que el trigger vea el estado actualizado

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
        } catch (Exception e) {
            log.error("Error en seatReservation: tableId={}, request={}", tableId, request, e);
            throw e;
        }
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

    /**
     * Sienta al cliente sugerido de la lista de espera en la mesa especificada.
     * Cambia el estado de la mesa a 'OCUPADA', marca al cliente como sentado (saliendo de la lista de espera)
     * y apertura la cuenta de consumo para la mesa.
     */
    @Transactional
    public SeatWaitlistResponse seatWaitlistEntry(
            Long tableId,
            SeatWaitlistRequest request,
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

        // Verificar si la mesa está bloqueada por una reserva activa vigente
        List<Reservation> activeReservations = reservationRepository
                .findActiveReservationsForTable(table.getId(), now.minusMinutes(45), now.plusMinutes(15));
        if (!activeReservations.isEmpty()) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "table_reserved_for_another_client",
                    "Mesa bloqueada por reserva",
                    "La mesa está reservada para otro horario/cliente"
            );
        }

        WaitlistEntry targetEntry;

        if (request != null && request.listaEsperaId() != null) {
            WaitlistEntry entry = waitlistRepository
                    .findByIdAndRestaurantId(request.listaEsperaId(), restaurantId)
                    .orElseThrow(() -> new ApiException(
                            HttpStatus.NOT_FOUND,
                            "waitlist_entry_not_found",
                            "Cliente en espera no encontrado",
                            "El cliente seleccionado no existe"
                    ));

            if (entry.getStatus() == WaitlistStatus.SENTADA || entry.getStatus() == WaitlistStatus.RETIRADA) {
                throw new ApiException(
                        HttpStatus.CONFLICT,
                        "waitlist_entry_not_active",
                        "Cliente no activo en lista",
                        "El cliente ya no se encuentra esperando en la lista"
                );
            }

            if (entry.getSuggestedTable() != null && !entry.getSuggestedTable().getId().equals(table.getId())) {
                throw new ApiException(
                        HttpStatus.CONFLICT,
                        "waitlist_entry_not_for_table",
                        "Mesa no coincidente",
                        "El cliente sugerido corresponde a otra mesa"
                );
            }

            if (entry.getStatus() == WaitlistStatus.ESPERANDO) {
                entry.markSuggested(table);
            }

            targetEntry = entry;

        } else {
            // Buscar sugerencia activa asignada a la mesa
            Optional<WaitlistEntry> activeSuggestion = waitlistRepository
                    .findActiveSuggestionForTable(
                            restaurantId,
                            table.getId(),
                            List.of(WaitlistStatus.SUGERIDA, WaitlistStatus.NOTIFICADA)
                    );

            if (activeSuggestion.isPresent()) {
                targetEntry = activeSuggestion.get();
            } else {
                // Intentar sugerir el siguiente cliente compatible si la mesa se acaba de liberar
                Long suggestedId = null;
                try {
                    suggestedId = waitlistRepository.suggestNextCompatible(restaurantId, table.getId());
                } catch (Exception ignored) {
                }

                if (suggestedId != null) {
                    targetEntry = waitlistRepository
                            .findByIdAndRestaurantId(suggestedId, restaurantId)
                            .orElse(null);
                } else {
                    targetEntry = null;
                }

                if (targetEntry == null) {
                    List<WaitlistEntry> compatibleList = waitlistRepository
                            .findCompatibleWaiting(restaurantId, table.getCapacity());
                    if (!compatibleList.isEmpty()) {
                        targetEntry = compatibleList.get(0);
                        targetEntry.markSuggested(table);
                    }
                }

                if (targetEntry == null) {
                    throw new ApiException(
                            HttpStatus.CONFLICT,
                            "no_waitlist_suggestion",
                            "Sin cliente sugerido",
                            "No existe ningún cliente en lista de espera compatible con esta mesa"
                    );
                }
            }
        }

        if (table.getCapacity() < targetEntry.getPeopleCount()) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "insufficient_capacity",
                    "Capacidad insuficiente",
                    "La mesa no tiene capacidad suficiente para el grupo"
            );
        }

        // Cambiar estado de la mesa a 'OCUPADA'
        table.occupy();
        tableRepository.save(table);

        // El cliente sale de la lista de espera al marcarse como 'SENTADA'
        targetEntry.markSeated();
        targetEntry.setSuggestedTable(table);
        WaitlistEntry savedEntry = waitlistRepository.save(targetEntry);
        waitlistRepository.flush(); // forzar flush para que el trigger vea el estado actualizado

        // Crear o asociar cuenta activa en la mesa
        short peopleCount = (request != null && request.cantidadPersonas() != null && request.cantidadPersonas() > 0)
                ? request.cantidadPersonas()
                : targetEntry.getPeopleCount();

        Optional<Account> existingAccountOpt = accountRepository
                .findActiveByTableIdAndRestaurantId(table.getId(), restaurantId);

        Account account;
        if (existingAccountOpt.isPresent()) {
            account = existingAccountOpt.get();
            if (account.getWaitlistId() == null) {
                account.setWaitlistId(savedEntry.getId());
                accountRepository.save(account);
            }
        } else {
            String accountNumber = generateAccountNumber(restaurantId, table.getNumber());
            account = new Account(restaurantId, table.getId(), context.userId(), accountNumber, peopleCount);
            account.setWaitlistId(savedEntry.getId());
            if (request != null && request.notas() != null && !request.notas().trim().isEmpty()) {
                account.setNotes(request.notas().trim());
            } else if (savedEntry.getNotes() != null) {
                account.setNotes(savedEntry.getNotes());
            }
            account = accountRepository.save(account);
        }

        return new SeatWaitlistResponse(
                "Cliente de la lista de espera sentado exitosamente. La mesa cambió a estado ocupada.",
                table.getId(),
                table.getNumber(),
                TableStatus.OCUPADA,
                savedEntry.getId(),
                savedEntry.getCustomerName(),
                savedEntry.getCustomerPhone(),
                peopleCount,
                WaitlistStatus.SENTADA,
                account.getId(),
                account.getAccountNumber(),
                Instant.now()
        );
    }

    /**
     * Sienta a un cliente sugerido de la lista de espera identificándolo directamente por su ID en la lista.
     */
    @Transactional
    public SeatWaitlistResponse seatWaitlistEntryById(
            Long waitlistId,
            SeatWaitlistRequest request,
            Authentication authentication) {

        AuthenticatedUser context = getAuthenticatedUser(authentication);
        WaitlistEntry entry = waitlistRepository
                .findByIdAndRestaurantId(waitlistId, context.restaurantId())
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "waitlist_entry_not_found",
                        "Cliente en espera no encontrado",
                        "El cliente seleccionado no existe"
                ));

        if (entry.getStatus() == WaitlistStatus.SENTADA || entry.getStatus() == WaitlistStatus.RETIRADA) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "waitlist_entry_not_active",
                    "Cliente no activo en lista",
                    "El cliente ya no se encuentra esperando en la lista"
            );
        }

        if (entry.getSuggestedTable() == null) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "waitlist_entry_without_table",
                    "Sugerencia sin mesa",
                    "El cliente no tiene una mesa sugerida asignada"
            );
        }

        SeatWaitlistRequest effectiveRequest = new SeatWaitlistRequest(
                entry.getId(),
                request != null && request.cantidadPersonas() != null ? request.cantidadPersonas() : entry.getPeopleCount(),
                request != null ? request.notas() : entry.getNotes()
        );

        return seatWaitlistEntry(entry.getSuggestedTable().getId(), effectiveRequest, authentication);
    }

    /**
     * Consulta el cliente sugerido de la lista de espera para una mesa liberada o genera una sugerencia si está disponible.
     */
    @Transactional
    public WaitlistSuggestionResponse getWaitlistSuggestionForTable(
            Long tableId,
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

        Optional<WaitlistEntry> activeSuggestion = waitlistRepository
                .findActiveSuggestionForTable(
                        restaurantId,
                        table.getId(),
                        List.of(WaitlistStatus.SUGERIDA, WaitlistStatus.NOTIFICADA)
                );

        WaitlistEntry entry = activeSuggestion.orElse(null);

        if (entry == null && table.getStatus() == TableStatus.LIBRE) {
            Long suggestedId = null;
            try {
                suggestedId = waitlistRepository.suggestNextCompatible(restaurantId, table.getId());
            } catch (Exception ignored) {
            }

            if (suggestedId != null) {
                entry = waitlistRepository.findByIdAndRestaurantId(suggestedId, restaurantId).orElse(null);
            }

            if (entry == null) {
                List<WaitlistEntry> compatibleList = waitlistRepository
                        .findCompatibleWaiting(restaurantId, table.getCapacity());
                if (!compatibleList.isEmpty()) {
                    entry = compatibleList.get(0);
                    entry.markSuggested(table);
                    entry = waitlistRepository.save(entry);
                }
            }
        }

        if (entry == null) {
            return null;
        }

        return toWaitlistSuggestionResponse(entry, table, restaurantId);
    }

    private WaitlistSuggestionResponse toWaitlistSuggestionResponse(
            WaitlistEntry entry,
            RestaurantTable table,
            Long restaurantId) {

        List<WaitlistEntry> queue = waitlistRepository
                .findAllByRestaurantIdAndStatusInOrderByArrivalTimeAscIdAsc(
                        restaurantId,
                        List.of(WaitlistStatus.ESPERANDO, WaitlistStatus.SUGERIDA, WaitlistStatus.NOTIFICADA)
                );

        int position = 0;
        for (int i = 0; i < queue.size(); i++) {
            if (queue.get(i).getId().equals(entry.getId())) {
                position = i + 1;
                break;
            }
        }

        OffsetDateTime arrival = entry.getArrivalTime()
                .atZoneSameInstant(GUATEMALA)
                .toOffsetDateTime();

        return new WaitlistSuggestionResponse(
                entry.getId(),
                position,
                entry.getCustomerName(),
                entry.getCustomerPhone(),
                entry.getPeopleCount(),
                arrival,
                entry.getStatus(),
                table.getId(),
                table.getNumber(),
                table.getCapacity()
        );
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

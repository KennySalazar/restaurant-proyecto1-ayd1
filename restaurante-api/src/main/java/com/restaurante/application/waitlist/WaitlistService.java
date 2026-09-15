package com.restaurante.application.waitlist;

import com.restaurante.application.reservation.ReservationTableBlockingService;
import com.restaurante.domain.model.RestaurantUserProfile;
import com.restaurante.domain.model.TableStatus;
import com.restaurante.domain.model.WaitlistEntry;
import com.restaurante.domain.repository.RestaurantTableRepository;
import com.restaurante.domain.repository.RestaurantUserProfileRepository;
import com.restaurante.domain.repository.WaitlistRepository;
import com.restaurante.exception.ApiException;
import com.restaurante.security.JwtData;
import com.restaurante.web.dto.waitlist.CreateWaitlistEntryRequest;
import com.restaurante.web.dto.waitlist.WaitlistEntryResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import com.restaurante.domain.model.WaitlistStatus;
import com.restaurante.web.dto.waitlist.WaitlistQueueEntryResponse;

import java.util.List;
import com.restaurante.domain.model.RestaurantTable;
import com.restaurante.web.dto.waitlist.WaitlistSuggestionResponse;

@Service
public class WaitlistService {

    private static final ZoneId GUATEMALA =
            ZoneId.of("America/Guatemala");

    private final WaitlistRepository waitlist;
    private final RestaurantTableRepository tables;
    private final RestaurantUserProfileRepository profiles;
    private final ReservationTableBlockingService reservationBlockingService;

    public WaitlistService(
            WaitlistRepository waitlist,
            RestaurantTableRepository tables,
            RestaurantUserProfileRepository profiles,
            ReservationTableBlockingService reservationBlockingService) {

        this.waitlist = waitlist;
        this.tables = tables;
        this.profiles = profiles;
        this.reservationBlockingService = reservationBlockingService;
    }

    @Transactional
    public WaitlistEntryResponse createWaitlistEntry(
            CreateWaitlistEntryRequest request,
            Authentication authentication) {

        AuthenticatedRestaurant context =
                getAuthenticatedRestaurant(authentication);

        reservationBlockingService.synchronizeReservationBlocks();

        boolean compatibleTableAvailable =
                tables.existsByRestaurantIdAndActiveTrueAndStatusAndCapacityGreaterThanEqual(
                        context.restaurantId(),
                        TableStatus.LIBRE,
                        request.cantidadPersonas()
                );

        if (compatibleTableAvailable) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "compatible_table_available",
                    "Mesa compatible disponible",
                    "Existe una mesa libre con capacidad suficiente para el grupo"
            );
        }

        OffsetDateTime arrivalTime =
                OffsetDateTime.now(GUATEMALA);

        String notes = request.notas();

        if (notes != null) {
            notes = notes.trim();

            if (notes.isEmpty()) {
                notes = null;
            }
        }

        WaitlistEntry entry = new WaitlistEntry(
                context.restaurantId(),
                request.nombreCliente().trim(),
                request.telefonoCliente().trim(),
                request.cantidadPersonas(),
                arrivalTime,
                notes,
                context.userId()
        );

        WaitlistEntry saved =
                waitlist.saveAndFlush(entry);

        return toResponse(saved);
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

    private WaitlistEntryResponse toResponse(
            WaitlistEntry entry) {

        OffsetDateTime arrival =
                entry.getArrivalTime()
                        .atZoneSameInstant(GUATEMALA)
                        .toOffsetDateTime();

        return new WaitlistEntryResponse(
                entry.getId(),
                entry.getCustomerName(),
                entry.getCustomerPhone(),
                entry.getPeopleCount(),
                arrival,
                entry.getStatus(),
                entry.getNotes()
        );
    }

    private record AuthenticatedRestaurant(
            Long userId,
            Long restaurantId
    ) {
    }
    private static final List<WaitlistStatus> ACTIVE_QUEUE_STATUSES =
            List.of(
                    WaitlistStatus.ESPERANDO,
                    WaitlistStatus.SUGERIDA,
                    WaitlistStatus.NOTIFICADA
            );
    @Transactional(readOnly = true)
    public List<WaitlistQueueEntryResponse> getWaitlist(
            Authentication authentication) {

        AuthenticatedRestaurant context =
                getAuthenticatedRestaurant(authentication);

        List<WaitlistEntry> entries =
                waitlist
                        .findAllByRestaurantIdAndStatusInOrderByArrivalTimeAscIdAsc(
                                context.restaurantId(),
                                ACTIVE_QUEUE_STATUSES
                        );

        return toQueueResponses(entries);
    }

    @Transactional(readOnly = true)
    public WaitlistQueueEntryResponse getWaitlistEntry(
            Long waitlistId,
            Authentication authentication) {

        AuthenticatedRestaurant context =
                getAuthenticatedRestaurant(authentication);

        List<WaitlistEntry> entries =
                waitlist
                        .findAllByRestaurantIdAndStatusInOrderByArrivalTimeAscIdAsc(
                                context.restaurantId(),
                                ACTIVE_QUEUE_STATUSES
                        );

        for (int i = 0; i < entries.size(); i++) {

            WaitlistEntry entry = entries.get(i);

            if (entry.getId().equals(waitlistId)) {
                return toQueueResponse(
                        entry,
                        i + 1
                );
            }
        }

        throw new ApiException(
                HttpStatus.NOT_FOUND,
                "waitlist_entry_not_found",
                "Cliente en espera no encontrado",
                "El cliente seleccionado no se encuentra actualmente en la lista de espera"
        );
    }

    private List<WaitlistQueueEntryResponse> toQueueResponses(
            List<WaitlistEntry> entries) {

        return java.util.stream.IntStream
                .range(0, entries.size())
                .mapToObj(index ->
                        toQueueResponse(
                                entries.get(index),
                                index + 1
                        ))
                .toList();
    }

    private WaitlistQueueEntryResponse toQueueResponse(
            WaitlistEntry entry,
            int position) {

        OffsetDateTime arrival =
                entry.getArrivalTime()
                        .atZoneSameInstant(GUATEMALA)
                        .toOffsetDateTime();

        return new WaitlistQueueEntryResponse(
                entry.getId(),
                position,
                entry.getCustomerName(),
                entry.getCustomerPhone(),
                entry.getPeopleCount(),
                arrival,
                entry.getStatus(),
                entry.getNotes()
        );
    }

    @Transactional
    public WaitlistSuggestionResponse suggestNextCompatible(
            Long tableId,
            Authentication authentication) {

        AuthenticatedRestaurant context =
                getAuthenticatedRestaurant(authentication);

        Long waitlistId;

        try {
            waitlistId = waitlist.suggestNextCompatible(
                    context.restaurantId(),
                    tableId
            );
        } catch (Exception ex) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "waitlist_suggestion_not_available",
                    "No fue posible generar la sugerencia",
                    "La mesa no está disponible para generar una sugerencia"
            );
        }

        if (waitlistId == null) {
            return null;
        }

        WaitlistEntry entry = waitlist
                .findByIdAndRestaurantId(
                        waitlistId,
                        context.restaurantId()
                )
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "waitlist_entry_not_found",
                        "Cliente en espera no encontrado",
                        "No fue posible recuperar el cliente sugerido"
                ));

        return toSuggestionResponse(
                entry,
                context.restaurantId()
        );
    }

    @Transactional
    public WaitlistSuggestionResponse confirmSuggestion(
            Long waitlistId,
            Authentication authentication) {

        AuthenticatedRestaurant context =
                getAuthenticatedRestaurant(authentication);

        WaitlistEntry entry = waitlist
                .findByIdAndRestaurantId(
                        waitlistId,
                        context.restaurantId()
                )
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "waitlist_entry_not_found",
                        "Cliente en espera no encontrado",
                        "El cliente seleccionado no existe"
                ));

        if (entry.getStatus() != WaitlistStatus.SUGERIDA
                && entry.getStatus() != WaitlistStatus.NOTIFICADA) {

            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "waitlist_entry_not_suggested",
                    "Cliente no sugerido",
                    "El cliente debe tener una sugerencia activa para confirmar la asignación"
            );
        }

        RestaurantTable table = entry.getSuggestedTable();
        if (table != null) {
            table.occupy();
            tables.save(table);
        }

        entry.markSeated();

        WaitlistEntry saved =
                waitlist.saveAndFlush(entry);

        return toSuggestionResponse(
                saved,
                context.restaurantId()
        );
    }
    @Transactional
    public WaitlistQueueEntryResponse rejectSuggestion(
            Long waitlistId,
            Authentication authentication) {

        AuthenticatedRestaurant context =
                getAuthenticatedRestaurant(authentication);

        WaitlistEntry entry = waitlist
                .findByIdAndRestaurantId(
                        waitlistId,
                        context.restaurantId()
                )
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "waitlist_entry_not_found",
                        "Cliente en espera no encontrado",
                        "El cliente seleccionado no existe"
                ));

        if (entry.getStatus() != WaitlistStatus.SUGERIDA
                && entry.getStatus() != WaitlistStatus.NOTIFICADA) {

            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "waitlist_entry_not_suggested",
                    "Cliente no sugerido",
                    "El cliente no tiene una sugerencia activa"
            );
        }

        entry.returnToWaiting();

        waitlist.saveAndFlush(entry);

        return getWaitlistEntry(
                waitlistId,
                authentication
        );
    }

    private WaitlistSuggestionResponse toSuggestionResponse(
            WaitlistEntry entry,
            Long restaurantId) {

        RestaurantTable table =
                entry.getSuggestedTable();

        if (table == null) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "waitlist_suggestion_without_table",
                    "Sugerencia sin mesa",
                    "La sugerencia no tiene una mesa asociada"
            );
        }

        List<WaitlistEntry> queue =
                waitlist
                        .findAllByRestaurantIdAndStatusInOrderByArrivalTimeAscIdAsc(
                                restaurantId,
                                ACTIVE_QUEUE_STATUSES
                        );

        int position = 0;

        for (int i = 0; i < queue.size(); i++) {
            if (queue.get(i).getId().equals(entry.getId())) {
                position = i + 1;
                break;
            }
        }

        OffsetDateTime arrival =
                entry.getArrivalTime()
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

    @Transactional
    public void evaluateFreeTable(
            Long restaurantId,
            Long tableId) {

        try {
            waitlist.suggestNextCompatible(
                    restaurantId,
                    tableId
            );
        } catch (Exception ignored) {
        }
    }
}
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
}
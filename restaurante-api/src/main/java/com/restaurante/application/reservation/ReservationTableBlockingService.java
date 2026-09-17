package com.restaurante.application.reservation;

import com.restaurante.domain.model.Reservation;
import com.restaurante.domain.model.RestaurantTable;
import com.restaurante.domain.model.TableStatus;
import com.restaurante.domain.repository.ReservationRepository;
import com.restaurante.domain.repository.RestaurantTableRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
public class ReservationTableBlockingService {

    private static final ZoneId GUATEMALA =
            ZoneId.of("America/Guatemala");

    private final ReservationRepository reservations;
    private final RestaurantTableRepository tables;

    public ReservationTableBlockingService(
            ReservationRepository reservations,
            RestaurantTableRepository tables) {
        this.reservations = reservations;
        this.tables = tables;
    }

    @Transactional
    public void synchronizeReservationBlocks() {

        OffsetDateTime now =
                OffsetDateTime.now(GUATEMALA);

        List<Reservation> activeReservations =
                reservations.findActiveReservationsAt(now);

        for (Reservation reservation : activeReservations) {

            RestaurantTable table =
                    reservation.getTable();

            if (table.isActive()
                    && table.getStatus() == TableStatus.LIBRE) {

                table.reserve();
                tables.save(table);
            }
        }

        List<RestaurantTable> reservedTables =
                tables.findAllByActiveTrueAndStatus(
                        TableStatus.RESERVADA
                );

        for (RestaurantTable table : reservedTables) {

            boolean hasActiveReservation =
                    reservations.hasActiveReservationAt(
                            table.getId(),
                            now
                    );

            boolean hasActiveWaitlistSuggestion =
                    tables.hasActiveWaitlistSuggestion(
                            table.getId()
                    );

            if (!hasActiveReservation
                    && !hasActiveWaitlistSuggestion) {

                table.releaseReservation();
                tables.save(table);
            }
        }
    }
}
package com.restaurante.domain.repository;

import com.restaurante.domain.model.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface ReservationRepository
        extends JpaRepository<Reservation, Long> {

    boolean existsByRestaurantIdAndReservationCode(
            Long restaurantId,
            String reservationCode
    );

    @Query(value = """
        SELECT EXISTS (
            SELECT 1
            FROM restaurante.reservas r
            WHERE r.mesa_id = :tableId
              AND r.estado IN (
                  'PENDIENTE',
                  'CONFIRMADA',
                  'CLIENTE_PRESENTE'
              )
              AND r.fecha_hora_inicio < :endDateTime
              AND r.fecha_hora_fin > :startDateTime
        )
        """, nativeQuery = true)
    boolean hasScheduleConflict(
            @Param("tableId") Long tableId,
            @Param("startDateTime") OffsetDateTime startDateTime,
            @Param("endDateTime") OffsetDateTime endDateTime
    );

    Optional<Reservation> findByIdAndRestaurantId(
            Long id,
            Long restaurantId
    );

    List<Reservation> findAllByRestaurantIdOrderByStartDateTimeAsc(
            Long restaurantId
    );

    List<Reservation> findAllByRestaurantIdAndStartDateTimeGreaterThanEqualAndStartDateTimeLessThanOrderByStartDateTimeAsc(
            Long restaurantId,
            OffsetDateTime start,
            OffsetDateTime end
    );
}
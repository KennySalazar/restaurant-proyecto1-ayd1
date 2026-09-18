package com.restaurante.domain.repository;

import com.restaurante.domain.model.Reservation;
import com.restaurante.domain.model.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Collection;
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

    Optional<Reservation> findByRestaurantIdAndReservationCode(
            Long restaurantId,
            String reservationCode
    );

    @Query("""
    SELECT r
    FROM Reservation r
    WHERE r.table.id = :tableId
      AND r.status IN (
        com.restaurante.domain.model.ReservationStatus.PENDIENTE,
        com.restaurante.domain.model.ReservationStatus.CONFIRMADA,
        com.restaurante.domain.model.ReservationStatus.CLIENTE_PRESENTE
      )
      AND r.startDateTime <= :endWindow
      AND r.endDateTime >= :startWindow
    ORDER BY r.startDateTime ASC
    """)
    List<Reservation> findActiveReservationsForTable(
            @Param("tableId") Long tableId,
            @Param("startWindow") OffsetDateTime startWindow,
            @Param("endWindow") OffsetDateTime endWindow
    );

    List<Reservation> findAllByRestaurantIdOrderByStartDateTimeAsc(
            Long restaurantId
    );

    List<Reservation> findAllByRestaurantIdAndStartDateTimeGreaterThanEqualAndStartDateTimeLessThanOrderByStartDateTimeAsc(
            Long restaurantId,
            OffsetDateTime start,
            OffsetDateTime end
    );

    List<Reservation> findAllByRestaurantIdAndStatusInAndStartDateTimeGreaterThanEqualOrderByStartDateTimeAsc(
            Long restaurantId,
            Collection<ReservationStatus> statuses,
            OffsetDateTime from
    );

    List<Reservation> findByRestaurantIdAndStatusInAndEndDateTimeAfterOrderByStartDateTimeAsc(
            Long restaurantId,
            Collection<ReservationStatus> statuses,
            OffsetDateTime now
    );

    @Query("""
    SELECT r
    FROM Reservation r
    WHERE r.status IN (
        com.restaurante.domain.model.ReservationStatus.PENDIENTE,
        com.restaurante.domain.model.ReservationStatus.CONFIRMADA,
        com.restaurante.domain.model.ReservationStatus.CLIENTE_PRESENTE
    )
      AND r.startDateTime <= :now
      AND r.endDateTime > :now
    """)
    List<Reservation> findActiveReservationsAt(
            @Param("now") OffsetDateTime now
    );

    @Query("""
    SELECT COUNT(r) > 0
    FROM Reservation r
    WHERE r.table.id = :tableId
      AND r.status IN (
        com.restaurante.domain.model.ReservationStatus.PENDIENTE,
        com.restaurante.domain.model.ReservationStatus.CONFIRMADA,
        com.restaurante.domain.model.ReservationStatus.CLIENTE_PRESENTE
      )
      AND r.startDateTime <= :now
      AND r.endDateTime > :now
    """)
    boolean hasActiveReservationAt(
            @Param("tableId") Long tableId,
            @Param("now") OffsetDateTime now
    );
}
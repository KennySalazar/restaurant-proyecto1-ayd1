package com.restaurante.domain.repository;

import com.restaurante.domain.model.RestaurantTable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.restaurante.domain.model.TableStatus;
import com.restaurante.domain.projection.OccupancyPanelProjection;

public interface RestaurantTableRepository extends JpaRepository<RestaurantTable, Long> {

    boolean existsByRestaurantIdAndNumber(Long restaurantId, String number);

    boolean existsByRestaurantIdAndNumberAndIdNot(
            Long restaurantId,
            String number,
            Long id
    );

    Optional<RestaurantTable> findByIdAndRestaurantIdAndActiveTrue(
            Long id,
            Long restaurantId
    );

    List<RestaurantTable> findAllByRestaurantIdOrderByIdAsc(Long restaurantId);

    Optional<RestaurantTable> findByIdAndRestaurantId(
            Long id,
            Long restaurantId
    );

    @Query(value = """
    SELECT EXISTS (
        SELECT 1
        FROM restaurante.cuenta_mesas cm
        JOIN restaurante.cuentas c
          ON c.id = cm.cuenta_id
        WHERE cm.mesa_id = :tableId
          AND cm.activa = TRUE
          AND c.estado IN (
              'ABIERTA',
              'LISTA_COBRO',
              'PARCIALMENTE_PAGADA'
          )
    )
    """, nativeQuery = true)
    boolean hasActiveAccount(@Param("tableId") Long tableId);

    @Query(value = """
    SELECT EXISTS (
        SELECT 1
        FROM restaurante.cuenta_mesas cm
        JOIN restaurante.comandas co
          ON co.cuenta_id = cm.cuenta_id
        WHERE cm.mesa_id = :tableId
          AND cm.activa = TRUE
          AND co.estado IN (
              'BORRADOR',
              'RECIBIDA',
              'EN_PREPARACION',
              'LISTA'
          )
    )
    """, nativeQuery = true)
    boolean hasActiveOrder(@Param("tableId") Long tableId);

    List<RestaurantTable> findAllByActiveTrueAndStatus(
            TableStatus status
    );

    boolean existsByRestaurantIdAndActiveTrueAndStatusAndCapacityGreaterThanEqual(
            Long restaurantId,
            TableStatus status,
            Short capacity
    );

    @Query(value = """
    SELECT EXISTS (
        SELECT 1
        FROM restaurante.lista_espera le
        WHERE le.mesa_sugerida_id = :tableId
          AND le.estado IN ('SUGERIDA', 'NOTIFICADA')
    )
    """, nativeQuery = true)
    boolean hasActiveWaitlistSuggestion(
            @Param("tableId") Long tableId
    );

    @Query(value = """
    SELECT
        mesa_id AS "mesaId",
        restaurante_id AS "restauranteId",
        numero,
        capacidad,
        zona,
        estado_visual AS "estadoVisual",
        cuenta_actual_id AS "cuentaActualId",
        numero_cuenta AS "numeroCuenta",
        es_principal AS "esPrincipal",
        reserva_actual_id AS "reservaActualId",
        cliente_reserva AS "clienteReserva",
        reserva_hasta AS "reservaHasta",
        lista_espera_actual_id AS "listaEsperaActualId",
        cliente_lista_espera AS "clienteListaEspera"
    FROM restaurante.vw_ocupacion_mesas_actual
    WHERE restaurante_id = :restaurantId
    ORDER BY zona, numero
    """, nativeQuery = true)
    List<OccupancyPanelProjection> findOccupancyPanel(
            @Param("restaurantId") Long restaurantId
    );


}


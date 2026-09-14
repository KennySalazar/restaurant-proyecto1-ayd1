package com.restaurante.domain.repository;

import com.restaurante.domain.model.WaitlistEntry;
import com.restaurante.domain.model.WaitlistStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import java.util.Collection;
import java.util.List;

public interface WaitlistRepository
        extends JpaRepository<WaitlistEntry, Long> {

    List<WaitlistEntry>
    findAllByRestaurantIdAndStatusInOrderByArrivalTimeAscIdAsc(
            Long restaurantId,
            Collection<WaitlistStatus> statuses
    );

    @Query(value = """
    SELECT restaurante.fn_sugerir_siguiente_lista_espera(
        :restaurantId,
        :tableId
    )
    """, nativeQuery = true)
    Long suggestNextCompatible(
            @Param("restaurantId") Long restaurantId,
            @Param("tableId") Long tableId
    );

    Optional<WaitlistEntry> findByIdAndRestaurantId(
            Long id,
            Long restaurantId
    );
}
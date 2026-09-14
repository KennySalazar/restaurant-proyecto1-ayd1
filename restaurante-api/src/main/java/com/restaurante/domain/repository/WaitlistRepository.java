package com.restaurante.domain.repository;

import com.restaurante.domain.model.WaitlistEntry;
import com.restaurante.domain.model.WaitlistStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface WaitlistRepository
        extends JpaRepository<WaitlistEntry, Long> {

    List<WaitlistEntry>
    findAllByRestaurantIdAndStatusInOrderByArrivalTimeAscIdAsc(
            Long restaurantId,
            Collection<WaitlistStatus> statuses
    );
}
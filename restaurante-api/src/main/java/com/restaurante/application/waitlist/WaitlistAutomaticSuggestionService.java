package com.restaurante.application.waitlist;

import com.restaurante.domain.repository.WaitlistRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WaitlistAutomaticSuggestionService {

    private final WaitlistRepository waitlist;

    public WaitlistAutomaticSuggestionService(
            WaitlistRepository waitlist) {

        this.waitlist = waitlist;
    }

    @Transactional
    public void evaluateTable(
            Long restaurantId,
            Long tableId) {

        waitlist.suggestNextCompatible(
                restaurantId,
                tableId
        );
    }
}
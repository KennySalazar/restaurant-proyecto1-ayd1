package com.restaurante.application.waitlist;

import com.restaurante.domain.model.RestaurantTable;
import com.restaurante.domain.model.TableStatus;
import com.restaurante.domain.repository.RestaurantTableRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class WaitlistSuggestionScheduler {

    private final RestaurantTableRepository tables;
    private final WaitlistAutomaticSuggestionService suggestionService;

    public WaitlistSuggestionScheduler(
            RestaurantTableRepository tables,
            WaitlistAutomaticSuggestionService suggestionService) {

        this.tables = tables;
        this.suggestionService = suggestionService;
    }

    @Scheduled(fixedDelay = 5000)
    public void evaluateWaitingCustomers() {

        List<RestaurantTable> freeTables =
                tables.findAllByActiveTrueAndStatus(
                        TableStatus.LIBRE
                );

        for (RestaurantTable table : freeTables) {
            try {
                suggestionService.evaluateTable(
                        table.getRestaurantId(),
                        table.getId()
                );
            } catch (RuntimeException ignored) {
            }
        }
    }
}
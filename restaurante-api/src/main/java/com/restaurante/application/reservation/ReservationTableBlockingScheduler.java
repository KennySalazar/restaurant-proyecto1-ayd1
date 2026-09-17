package com.restaurante.application.reservation;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ReservationTableBlockingScheduler {

    private final ReservationTableBlockingService blockingService;

    public ReservationTableBlockingScheduler(
            ReservationTableBlockingService blockingService) {
        this.blockingService = blockingService;
    }

    @Scheduled(fixedDelay = 30000)
    public void synchronizeReservationBlocks() {
        blockingService.synchronizeReservationBlocks();
    }
}
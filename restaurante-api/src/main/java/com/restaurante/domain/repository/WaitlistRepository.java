package com.restaurante.domain.repository;

import com.restaurante.domain.model.WaitlistEntry;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WaitlistRepository
        extends JpaRepository<WaitlistEntry, Long> {
}
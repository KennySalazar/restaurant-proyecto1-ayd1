package com.restaurante.domain.repository;

import com.restaurante.domain.model.OtpChallenge;
import com.restaurante.domain.model.OtpPurpose;
import com.restaurante.domain.model.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OtpChallengeRepository extends JpaRepository<OtpChallenge, UUID> {

    Optional<OtpChallenge> findByIdAndPurpose(
            UUID id,
            OtpPurpose purpose
    );

    void deleteByUserAndPurpose(
            UserAccount user,
            OtpPurpose purpose
    );
}

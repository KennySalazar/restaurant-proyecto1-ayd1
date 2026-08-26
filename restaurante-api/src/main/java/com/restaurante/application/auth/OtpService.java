package com.restaurante.application.auth;

import com.restaurante.config.OtpProperties;
import com.restaurante.domain.model.OtpChallenge;
import com.restaurante.domain.model.OtpPurpose;
import com.restaurante.domain.model.UserAccount;
import com.restaurante.domain.repository.OtpChallengeRepository;
import com.restaurante.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
public class OtpService {

    private static final int OTP_BOUND = 1_000_000;

    private final OtpChallengeRepository challenges;
    private final PasswordEncoder passwordEncoder;
    private final OtpProperties properties;
    private final SecureRandom random = new SecureRandom();

    public OtpService(
            OtpChallengeRepository challenges,
            PasswordEncoder passwordEncoder,
            OtpProperties properties
    ) {
        this.challenges = challenges;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
    }

    @Transactional
    public OtpIssue issue(
            UserAccount user,
            OtpPurpose purpose
    ) {
        challenges.deleteByUserAndPurpose(user, purpose);

        String code = generateCode();

        Instant expiresAt = Instant.now().plus(
                properties.getExpirationMinutes(),
                ChronoUnit.MINUTES
        );

        OtpChallenge challenge = new OtpChallenge(
                user,
                purpose,
                passwordEncoder.encode(code),
                expiresAt
        );

        challenges.save(challenge);

        return new OtpIssue(
                challenge.getId(),
                code,
                expiresAt
        );
    }

    @Transactional(noRollbackFor = ApiException.class)
    public UserAccount verify(
            UUID challengeId,
            OtpPurpose purpose,
            String code
    ) {
        OtpChallenge challenge = challenges
                .findByIdAndPurpose(challengeId, purpose)
                .orElseThrow(this::invalidOtp);

        if (challenge.isConsumed()) {
            throw invalidOtp(
                    "El código ya fue utilizado"
            );
        }

        if (challenge.isExpired()) {
            throw invalidOtp(
                    "El código ha expirado"
            );
        }

        if (challenge.getAttempts() >= properties.getMaxAttempts()) {
            throw attemptsExceeded();
        }

        if (!passwordEncoder.matches(code, challenge.getCodeHash())) {
            challenge.incrementAttempts();

            if (challenge.getAttempts() >= properties.getMaxAttempts()) {
                challenge.consume();
                challenges.save(challenge);

                throw attemptsExceeded();
            }

            challenges.save(challenge);

            throw invalidOtp(
                    "El código ingresado no es válido"
            );
        }

        challenge.consume();
        challenges.save(challenge);

        return challenge.getUser();
    }

    public int expirationMinutes() {
        return properties.getExpirationMinutes();
    }

    private String generateCode() {
        return String.format(
                "%06d",
                random.nextInt(OTP_BOUND)
        );
    }

    private ApiException invalidOtp() {
        return invalidOtp(
                "El código es inválido, ya expiró o no existe"
        );
    }

    private ApiException invalidOtp(String detail) {
        return new ApiException(
                HttpStatus.BAD_REQUEST,
                "invalid_otp",
                "Código inválido",
                detail
        );
    }

    private ApiException attemptsExceeded() {
        return new ApiException(
                HttpStatus.TOO_MANY_REQUESTS,
                "otp_attempts_exceeded",
                "Demasiados intentos",
                "Se alcanzó el número máximo de intentos permitidos para este código"
        );
    }
}

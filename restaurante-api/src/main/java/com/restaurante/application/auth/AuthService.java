package com.restaurante.application.auth;

import com.restaurante.application.common.EmailNormalizer;
import com.restaurante.domain.model.OtpPurpose;
import com.restaurante.domain.model.UserAccount;
import com.restaurante.domain.repository.UserAccountRepository;
import com.restaurante.exception.ApiException;
import com.restaurante.security.JwtService;
import com.restaurante.web.dto.ChallengeResponse;
import com.restaurante.web.dto.ChangePasswordRequest;
import com.restaurante.web.dto.LoginResponse;
import com.restaurante.web.dto.MessageResponse;
import com.restaurante.web.dto.PasswordChangeResponse;
import com.restaurante.web.dto.RecoveryVerificationRequest;
import com.restaurante.web.dto.TwoFactorRequest;
import com.restaurante.web.dto.UserResponse;
import com.restaurante.web.dto.VerifyChallengeRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
public class AuthService {

    private final UserAccountRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final OtpService otpService;
    private final OtpDeliveryService otpDeliveryService;

    public AuthService(
            UserAccountRepository users,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            OtpService otpService,
            OtpDeliveryService otpDeliveryService
    ) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.otpService = otpService;
        this.otpDeliveryService = otpDeliveryService;
    }

    @Transactional
    public LoginResponse login(String rawEmail, String password) {
        UserAccount user = users
                .findByEmail(EmailNormalizer.normalize(rawEmail))
                .orElse(null);

        if (user == null || !passwordEncoder.matches(password, user.getPasswordHash())) {
            throw invalidCredentials();
        }

        ensureActive(user);

        if (user.isTwoFactorEnabled()) {
            OtpIssue issue = otpDeliveryService.send(
                    user,
                    OtpPurpose.LOGIN
            );

            return LoginResponse.challenge(
                    issue.challengeId(),
                    issue.expiresAt()
            );
        }

        return LoginResponse.token(
                jwtService.issue(user),
                jwtService.expirationTime()
        );
    }

    @Transactional
    public LoginResponse verifyLogin(
            VerifyChallengeRequest request
    ) {
        UserAccount user = otpService.verify(
                request.challengeId(),
                OtpPurpose.LOGIN,
                request.otp()
        );

        ensureActive(user);

        if (!user.isTwoFactorEnabled()) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "two_factor_not_enabled",
                    "Doble factor no habilitado",
                    "La autenticación de dos factores no está habilitada para esta cuenta"
            );
        }

        return LoginResponse.token(
                jwtService.issue(user),
                jwtService.expirationTime()
        );
    }

    @Transactional
    public ChallengeResponse requestRecovery(String rawEmail) {
        String email = EmailNormalizer.normalize(rawEmail);

        UserAccount user = users
                .findByEmail(email)
                .orElse(null);

        if (user != null && user.isEnabled()) {
            OtpIssue issue = otpDeliveryService.send(
                    user,
                    OtpPurpose.PASSWORD_RECOVERY
            );

            return new ChallengeResponse(
                    issue.challengeId(),
                    issue.expiresAt(),
                    recoveryMessage()
            );
        }

        /*
         * No revelamos si el correo existe o no.
         * Devolvemos un challenge ficticio para mantener una respuesta uniforme.
         */
        Instant expiresAt = Instant.now().plus(
                otpService.expirationMinutes(),
                ChronoUnit.MINUTES
        );

        return new ChallengeResponse(
                UUID.randomUUID(),
                expiresAt,
                recoveryMessage()
        );
    }

    @Transactional
    public MessageResponse verifyRecovery(
            RecoveryVerificationRequest request
    ) {
        ensurePasswordPolicy(request.newPassword());

        UserAccount user = otpService.verify(
                request.challengeId(),
                OtpPurpose.PASSWORD_RECOVERY,
                request.otp()
        );

        ensureActive(user);

        user.setPasswordHash(
                passwordEncoder.encode(request.newPassword())
        );

        user.incrementTokenVersion();
        users.save(user);

        return new MessageResponse(
                "La contraseña fue restablecida correctamente. Inicia sesión nuevamente."
        );
    }

    @Transactional
    public PasswordChangeResponse changePassword(
            UserAccount user,
            ChangePasswordRequest request
    ) {
        ensurePasswordPolicy(request.newPassword());
        ensureActive(user);

        if (!passwordEncoder.matches(
                request.currentPassword(),
                user.getPasswordHash()
        )) {
            throw invalidCredentials(
                    "La contraseña actual no es correcta"
            );
        }

        if (request.currentPassword().equals(request.newPassword())) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "invalid_password_transition",
                    "Contraseña inválida",
                    "La nueva contraseña debe ser diferente de la actual"
            );
        }

        user.setPasswordHash(
                passwordEncoder.encode(request.newPassword())
        );

        user.incrementTokenVersion();
        users.save(user);

        return new PasswordChangeResponse(
                jwtService.issue(user),
                "Bearer",
                jwtService.expirationTime(),
                "La contraseña fue cambiada correctamente"
        );
    }

    @Transactional
    public ChallengeResponse requestTwoFactorChange(
            UserAccount user,
            boolean enable,
            TwoFactorRequest request
    ) {
        ensureActive(user);

        if (!passwordEncoder.matches(
                request.currentPassword(),
                user.getPasswordHash()
        )) {
            throw invalidCredentials(
                    "La contraseña actual no es correcta"
            );
        }

        if (enable && user.isTwoFactorEnabled()) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "two_factor_already_enabled",
                    "Doble factor ya habilitado",
                    "La autenticación de dos factores ya está habilitada"
            );
        }

        if (!enable && !user.isTwoFactorEnabled()) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "two_factor_already_disabled",
                    "Doble factor ya deshabilitado",
                    "La autenticación de dos factores ya está deshabilitada"
            );
        }

        OtpPurpose purpose = enable
                ? OtpPurpose.TWO_FACTOR_ENABLE
                : OtpPurpose.TWO_FACTOR_DISABLE;

        OtpIssue issue = otpDeliveryService.send(
                user,
                purpose
        );

        return new ChallengeResponse(
                issue.challengeId(),
                issue.expiresAt(),
                "Se envió un código de verificación al correo electrónico"
        );
    }

    @Transactional
    public MessageResponse confirmTwoFactorChange(
            UserAccount authenticatedUser,
            boolean enable,
            VerifyChallengeRequest request
    ) {
        OtpPurpose purpose = enable
                ? OtpPurpose.TWO_FACTOR_ENABLE
                : OtpPurpose.TWO_FACTOR_DISABLE;

        UserAccount challengeUser = otpService.verify(
                request.challengeId(),
                purpose,
                request.otp()
        );

        if (!challengeUser.getId().equals(authenticatedUser.getId())) {
            throw new ApiException(
                    HttpStatus.FORBIDDEN,
                    "challenge_owner_mismatch",
                    "Código no válido",
                    "El código no pertenece a la cuenta autenticada"
            );
        }

        ensureActive(authenticatedUser);

        if (enable) {
            if (authenticatedUser.isTwoFactorEnabled()) {
                throw new ApiException(
                        HttpStatus.CONFLICT,
                        "two_factor_already_enabled",
                        "Doble factor ya habilitado",
                        "La autenticación de dos factores ya está habilitada"
                );
            }

            authenticatedUser.enableTwoFactor();
        } else {
            if (!authenticatedUser.isTwoFactorEnabled()) {
                throw new ApiException(
                        HttpStatus.CONFLICT,
                        "two_factor_already_disabled",
                        "Doble factor ya deshabilitado",
                        "La autenticación de dos factores ya está deshabilitada"
                );
            }

            authenticatedUser.disableTwoFactor();
        }

        users.save(authenticatedUser);

        return new MessageResponse(
                enable
                        ? "La autenticación de dos factores fue activada correctamente"
                        : "La autenticación de dos factores fue desactivada correctamente"
        );
    }

    @Transactional(readOnly = true)
    public UserResponse currentUser(String email) {
        return toResponse(requireUser(email));
    }

    @Transactional(readOnly = true)
    public UserAccount requireUser(String rawEmail) {
        return users
                .findByEmail(EmailNormalizer.normalize(rawEmail))
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND,
                        "account_not_found",
                        "Cuenta no encontrada",
                        "La cuenta no existe"
                ));
    }

    public UserResponse toResponse(UserAccount user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getRole().getName().name(),
                user.isEnabled(),
                user.isTwoFactorEnabled()
        );
    }

    private String recoveryMessage() {
        return "Si el correo pertenece a una cuenta activa, se envió un código de recuperación";
    }

    private void ensureActive(UserAccount user) {
        if (!user.isEnabled()) {
            throw new ApiException(
                    HttpStatus.FORBIDDEN,
                    "account_disabled",
                    "Cuenta deshabilitada",
                    "La cuenta está deshabilitada"
            );
        }
    }

    private ApiException invalidCredentials() {
        return invalidCredentials("Credenciales inválidas");
    }

    private ApiException invalidCredentials(String detail) {
        return new ApiException(
                HttpStatus.UNAUTHORIZED,
                "invalid_credentials",
                "Credenciales inválidas",
                detail
        );
    }

    private void ensurePasswordPolicy(String password) {
        if (!PasswordPolicy.isValid(password)) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "invalid_password",
                    "Contraseña inválida",
                    "La contraseña debe tener entre 8 y 72 caracteres, con mayúsculas, minúsculas y números"
            );
        }
    }
}

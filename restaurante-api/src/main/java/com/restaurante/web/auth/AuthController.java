package com.restaurante.web.auth;

import com.restaurante.application.auth.AuthService;
import com.restaurante.web.dto.ChallengeResponse;
import com.restaurante.web.dto.ChangePasswordRequest;
import com.restaurante.web.dto.LoginRequest;
import com.restaurante.web.dto.LoginResponse;
import com.restaurante.web.dto.MessageResponse;
import com.restaurante.web.dto.PasswordChangeResponse;
import com.restaurante.web.dto.RecoveryRequest;
import com.restaurante.web.dto.RecoveryVerificationRequest;
import com.restaurante.web.dto.TwoFactorRequest;
import com.restaurante.web.dto.UserResponse;
import com.restaurante.web.dto.VerifyChallengeRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@Tag(
        name = "Autenticación",
        description = "Inicio de sesión, recuperación de contraseña y autenticación de dos factores"
)
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    @Operation(
            summary = "Iniciar sesión",
            description = "Valida correo y contraseña. Si el usuario tiene 2FA activo, envía un OTP."
    )
    public LoginResponse login(
            @Valid @RequestBody LoginRequest request
    ) {
        return authService.login(
                request.email(),
                request.password()
        );
    }

    @PostMapping("/login/verify")
    @Operation(
            summary = "Verificar segundo factor",
            description = "Valida el OTP de inicio de sesión y devuelve el JWT"
    )
    public LoginResponse verifyLogin(
            @Valid @RequestBody VerifyChallengeRequest request
    ) {
        return authService.verifyLogin(request);
    }

    @PostMapping("/password-recovery")
    @Operation(
            summary = "Solicitar recuperación de contraseña",
            description = "Envía un código OTP si el correo corresponde a una cuenta activa"
    )
    public ChallengeResponse requestRecovery(
            @Valid @RequestBody RecoveryRequest request
    ) {
        return authService.requestRecovery(request.email());
    }

    @PostMapping("/password-recovery/verify")
    @Operation(
            summary = "Restablecer contraseña",
            description = "Verifica el OTP de recuperación y establece una nueva contraseña"
    )
    public MessageResponse verifyRecovery(
            @Valid @RequestBody RecoveryVerificationRequest request
    ) {
        return authService.verifyRecovery(request);
    }

    @PostMapping({"/password/change", "/change-password"})
    @Operation(
            summary = "Cambiar contraseña",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @SecurityRequirement(name = "bearerAuth")
    public PasswordChangeResponse changePassword(
            Authentication authentication,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        return authService.changePassword(
                authService.requireUser(authentication.getName()),
                request
        );
    }

    @PostMapping("/2fa/enable")
    @Operation(
            summary = "Solicitar activación de 2FA",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @SecurityRequirement(name = "bearerAuth")
    public ChallengeResponse requestTwoFactorEnable(
            Authentication authentication,
            @Valid @RequestBody TwoFactorRequest request
    ) {
        return authService.requestTwoFactorChange(
                authService.requireUser(authentication.getName()),
                true,
                request
        );
    }

    @PostMapping("/2fa/enable/verify")
    @Operation(
            summary = "Confirmar activación de 2FA",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @SecurityRequirement(name = "bearerAuth")
    public MessageResponse confirmTwoFactorEnable(
            Authentication authentication,
            @Valid @RequestBody VerifyChallengeRequest request
    ) {
        return authService.confirmTwoFactorChange(
                authService.requireUser(authentication.getName()),
                true,
                request
        );
    }

    @PostMapping("/2fa/disable")
    @Operation(
            summary = "Solicitar desactivación de 2FA",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @SecurityRequirement(name = "bearerAuth")
    public ChallengeResponse requestTwoFactorDisable(
            Authentication authentication,
            @Valid @RequestBody TwoFactorRequest request
    ) {
        return authService.requestTwoFactorChange(
                authService.requireUser(authentication.getName()),
                false,
                request
        );
    }

    @PostMapping("/2fa/disable/verify")
    @Operation(
            summary = "Confirmar desactivación de 2FA",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @SecurityRequirement(name = "bearerAuth")
    public MessageResponse confirmTwoFactorDisable(
            Authentication authentication,
            @Valid @RequestBody VerifyChallengeRequest request
    ) {
        return authService.confirmTwoFactorChange(
                authService.requireUser(authentication.getName()),
                false,
                request
        );
    }

    @GetMapping("/me")
    @Operation(
            summary = "Consultar la cuenta autenticada",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @SecurityRequirement(name = "bearerAuth")
    public UserResponse currentUser(
            Authentication authentication
    ) {
        return authService.currentUser(
                authentication.getName()
        );
    }
}

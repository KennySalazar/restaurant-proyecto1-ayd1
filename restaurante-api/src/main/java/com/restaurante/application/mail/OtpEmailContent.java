package com.restaurante.application.mail;

import com.restaurante.domain.model.OtpPurpose;

final class OtpEmailContent {

    private OtpEmailContent() {}

    //metodo para obtener los asuntos de los correos OTP
    static String subjectFor(OtpPurpose purpose) {
        return switch (purpose) {
            case LOGIN ->
                    "Código de inicio de sesión";
            case PASSWORD_RECOVERY ->
                    "Recuperación de contraseña";
            case TWO_FACTOR_ENABLE ->
                    "Activación de autenticación de dos factores";
            case TWO_FACTOR_DISABLE ->
                    "Desactivación de autenticación de dos factores";
        };
    }

    //metodo para obtener el cuerpo del correo
    static String bodyFor(
            String code,
            OtpPurpose purpose,
            int expirationMinutes
    ) {
        String action = switch (purpose) {
            case LOGIN ->
                    "completar tu inicio de sesión";
            case PASSWORD_RECOVERY ->
                    "recuperar tu contraseña";
            case TWO_FACTOR_ENABLE ->
                    "activar la autenticación de dos factores";
            case TWO_FACTOR_DISABLE ->
                    "desactivar la autenticación de dos factores";
        };

        return """
                Sistema de Gestión de Restaurante

                Utiliza el siguiente código para %s:

                %s

                Este código expira en %d minutos.

                Si no solicitaste esta operación, puedes ignorar este mensaje.
                """.formatted(action, code, expirationMinutes);
    }
}

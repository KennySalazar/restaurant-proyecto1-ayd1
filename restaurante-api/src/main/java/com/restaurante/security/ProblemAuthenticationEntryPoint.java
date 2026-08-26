package com.restaurante.security;

import com.restaurante.exception.ProblemDetails;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;

@Component
public class ProblemAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public ProblemAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException exception) throws IOException {
        response.setHeader("WWW-Authenticate", "Bearer");
        boolean invalidToken = exception instanceof BadCredentialsException;
        var problem = ProblemDetails.create(HttpServletResponse.SC_UNAUTHORIZED,
            "Autenticación requerida",
            invalidToken ? "El token JWT no es válido" : "Debe autenticarse para acceder a este recurso",
            invalidToken ? "invalid_token" : "authentication_required", request);
        ProblemDetails.write(response, objectMapper, problem);
    }
}

package com.restaurante.web.dto;

public record UserResponse(
        Long id,
        String email,
        String role,
        boolean enabled,
        boolean twoFactorEnabled
) {
}

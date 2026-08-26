package com.restaurante.web.dto;

public record PasswordChangeResponse(String accessToken, String tokenType, long expiresIn, String message) {
}

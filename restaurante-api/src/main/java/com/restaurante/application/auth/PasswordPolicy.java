package com.restaurante.application.auth;

public final class PasswordPolicy {

    public static final int MIN_LENGTH = 8;
    public static final int MAX_LENGTH = 72;

    private PasswordPolicy() {
    }

    public static boolean isValid(String password) {
        if (password == null || password.length() < MIN_LENGTH || password.length() > MAX_LENGTH) {
            return false;
        }
        boolean upper = false;
        boolean lower = false;
        boolean digit = false;
        for (int index = 0; index < password.length(); index++) {
            char character = password.charAt(index);
            upper |= Character.isUpperCase(character);
            lower |= Character.isLowerCase(character);
            digit |= Character.isDigit(character);
        }
        return upper && lower && digit;
    }
}

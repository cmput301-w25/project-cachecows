package com.example.feelink;

public class ValidationUtils {
    // Regex patterns (reusable constants)
    public static final String VALID_USERNAME_REGEX = "^(?=.*[a-zA-Z])[a-zA-Z0-9_]{3,25}$";
    public static final String VALID_PASSWORD_REGEX = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])[a-zA-Z0-9!@#$%^&*()_+\\-=]{6,}$";

    public static boolean isValidUsername(String username) {
        return username.matches(VALID_USERNAME_REGEX);
    }

    public static boolean isValidPassword(String password) {
        return password.matches(VALID_PASSWORD_REGEX);
    }
}
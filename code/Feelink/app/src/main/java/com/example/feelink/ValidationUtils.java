// ValidationUtils.java
package com.example.feelink;

import java.util.regex.Pattern;

public class ValidationUtils {
    // Username: 3-25 chars, letters/numbers/underscores, must contain at least 1 letter
    public static final String USERNAME_REGEX = "^(?=.*[a-zA-Z])[a-zA-Z0-9_]{3,25}$";

    // Password: 6+ chars, 1+ uppercase, 1+ lowercase, 1+ number
    public static final String PASSWORD_REGEX = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])[a-zA-Z0-9!@#$%^&*()_+\\-=]{6,}$";

    // Basic email pattern (similar to Android's Patterns.EMAIL_ADDRESS but pure Java)
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$", Pattern.CASE_INSENSITIVE);

    /**
     * Validates username format
     * @param username Input username
     * @return true if matches username requirements
     */
    public static boolean isUsernameValid(String username) {
        return username.matches(USERNAME_REGEX);
    }

    /**
     * Validates password complexity
     * @param password Input password
     * @return true if meets complexity requirements
     */
    public static boolean isPasswordValid(String password) {
        return password.matches(PASSWORD_REGEX);
    }

    /**
     * Validates email format
     * @param email Input email
     * @return true if valid email format
     */
    public static boolean isEmailValid(String email) {
        return EMAIL_PATTERN.matcher(email).matches();
    }

    /**
     * Checks if credentials are non-empty
     * @param username Login username
     * @param password Login password
     * @return true if both fields have content
     */
    public static boolean areCredentialsValid(String username, String password) {
        return !username.trim().isEmpty() && !password.trim().isEmpty();
    }

    /**
     * Checks if two passwords match
     * @param pass1 First password input
     * @param pass2 Second password input
     * @return true if identical
     */
    public static boolean arePasswordsMatching(String pass1, String pass2) {
        return pass1.equals(pass2);
    }
}
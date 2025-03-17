// ValidationUtils.java
package com.example.feelink;

import android.os.Build;

import androidx.annotation.RequiresApi;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.regex.Pattern;

public class ValidationUtils {
    // Username: 3-25 chars, letters/numbers/underscores, must contain at least 1 letter
    public static final String USERNAME_REGEX = "^(?=.*[a-zA-Z])[a-zA-Z0-9_]{3,25}$";

    // Password: 6+ chars, 1+ uppercase, 1+ lowercase, 1+ number
    public static final String PASSWORD_REGEX = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])[a-zA-Z0-9!@#$%^&*()_+\\-=]{6,}$";

    // Basic email pattern (similar to Android's Patterns.EMAIL_ADDRESS but pure Java)
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,6}$", Pattern.CASE_INSENSITIVE);

    public static final String DATE_REGEX = "^\\d{2}/\\d{2}/\\d{4}$";

    /**
     * Validates date format compliance with dd/mm/yyyy pattern
     *
     * @param date Input date string to validate
     * @return true if matches the dd/mm/yyyy format, false otherwise
     * @see #DATE_REGEX
     */
    // Updated ValidationUtils.java
    @RequiresApi(api = Build.VERSION_CODES.O)
    public static boolean isDateValid(String date) {
        if (!date.matches(DATE_REGEX)) return false;

        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/uuuu")
                    .withResolverStyle(ResolverStyle.STRICT);
            LocalDate.parse(date, formatter);
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }


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

    public static boolean isReasonNotValid(String text) {
        boolean exceedsCharLimit = text.length() > 20;
        // Check word limit
        String[] words = text.trim().split("\\s+");
        boolean exceedsWordLimit = text.trim().length() > 0 && words.length > 3;
        return (exceedsCharLimit || exceedsWordLimit);
    }
}
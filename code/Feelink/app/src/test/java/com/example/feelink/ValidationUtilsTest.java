package com.example.feelink;


import org.junit.Test;
import static org.junit.Assert.*;

public class ValidationUtilsTest {
    // Username Validation
    @Test
    public void testUsernameRequirements() {
        assertTrue("Valid username", ValidationUtils.isUsernameValid("user_123"));
        assertFalse("Too short (2 chars)", ValidationUtils.isUsernameValid("ab"));
        assertFalse("No letters", ValidationUtils.isUsernameValid("123"));
        assertFalse("Special chars", ValidationUtils.isUsernameValid("user@"));
    }

    // Password Validation
    @Test
    public void testPasswordRequirements() {
        assertTrue("Valid password", ValidationUtils.isPasswordValid("Pass12"));
        assertTrue("Special chars", ValidationUtils.isPasswordValid("Pa$$w0rd"));
        assertFalse("No uppercase", ValidationUtils.isPasswordValid("pass1"));
        assertFalse("No number", ValidationUtils.isPasswordValid("Password"));
        assertFalse("Too short (5 chars)", ValidationUtils.isPasswordValid("Pa1ss"));
    }

    // Credential Validation
    @Test
    public void testCredentialPresence() {
        assertTrue("Both filled", ValidationUtils.areCredentialsValid("user", "pass"));
        assertFalse("Empty username", ValidationUtils.areCredentialsValid("", "pass"));
        assertFalse("Empty password", ValidationUtils.areCredentialsValid("user", ""));
        assertFalse("Both empty", ValidationUtils.areCredentialsValid("", ""));
    }

    // Email Validation
    @Test
    public void testEmailPatterns() {
        assertTrue("Standard email", ValidationUtils.isEmailValid("test@example.com"));
        assertTrue("Subdomain email", ValidationUtils.isEmailValid("user@mail.co.uk"));
        assertFalse("Missing @", ValidationUtils.isEmailValid("invalid.email"));
        assertFalse("No domain", ValidationUtils.isEmailValid("user@"));
    }


    @Test
    public void testValidDateFormats() {
        // Proper slash format
        assertTrue("Standard date", ValidationUtils.isDateValid("31/12/2025"));
        assertTrue("Mid-month date", ValidationUtils.isDateValid("15/06/2024"));
        assertTrue("End of month", ValidationUtils.isDateValid("30/03/2025")); // Format valid (semantic validity not checked)
        assertTrue("Leap year format", ValidationUtils.isDateValid("29/02/2024")); // Format only
    }


    @Test
    public void testInvalidDateFormats() {
        // Wrong separators
        assertFalse("Hyphen separator", ValidationUtils.isDateValid("31-12-2025"));
        assertFalse("Mixed separators", ValidationUtils.isDateValid("15/12-2025"));

        // Digit count violations
        assertFalse("Single-digit day", ValidationUtils.isDateValid("5/12/2025"));
        assertFalse("3-digit month", ValidationUtils.isDateValid("15/123/2025"));
        assertFalse("Short year", ValidationUtils.isDateValid("15/12/25"));

        // Invalid characters
        assertFalse("Text month", ValidationUtils.isDateValid("15/Dec/2025"));
        assertFalse("Special characters", ValidationUtils.isDateValid("15/12/20$5"));

        // Edge cases
        assertFalse("Empty input", ValidationUtils.isDateValid(""));
    }

    @Test
    public void testImpossibleDates() {
        // Format valid but dates impossible
        assertFalse("February 30th", ValidationUtils.isDateValid("30/02/2025"));
        assertFalse("April 31st", ValidationUtils.isDateValid("31/04/2024"));
        assertFalse("Month 00", ValidationUtils.isDateValid("15/00/2025"));
        assertFalse("Day 00", ValidationUtils.isDateValid("00/12/2025"));
    }

    @Test
    public void testValidBoundaryDates() {
        assertTrue("Leap day", ValidationUtils.isDateValid("29/02/2024"));
        assertTrue("31-day month", ValidationUtils.isDateValid("31/01/2025"));
        assertTrue("30-day month", ValidationUtils.isDateValid("30/04/2025"));
    }

}
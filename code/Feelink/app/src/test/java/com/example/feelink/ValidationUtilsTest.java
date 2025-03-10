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
}
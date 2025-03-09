package com.example.feelink;

import static org.junit.Assert.*;

import org.junit.Test;

public class ValidationUtilsTest {

    @Test
    public void isValidUsername_ValidInput_ReturnsTrue() {
        assertTrue(ValidationUtils.isValidUsername("valid_user123"));
    }

    @Test
    public void isValidUsername_TooShort_ReturnsFalse() {
        assertFalse(ValidationUtils.isValidUsername("ab"));
    }

    @Test
    public void isValidPassword_ValidInput_ReturnsTrue() {
        assertTrue(ValidationUtils.isValidPassword("Pass123!"));
    }

    @Test
    public void isValidPassword_NoUppercase_ReturnsFalse() {
        assertFalse(ValidationUtils.isValidPassword("pass123!"));
    }
}
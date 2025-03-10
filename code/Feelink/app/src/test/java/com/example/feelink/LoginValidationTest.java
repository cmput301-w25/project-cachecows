package com.example.feelink;

import org.junit.Test;
import static org.junit.Assert.*;
import static com.example.feelink.ValidationUtils.*;

public class LoginValidationTest {
    // Test credential validation edge cases
    @Test
    public void testWhitespaceCredentials() {
        assertFalse("Username with spaces", areCredentialsValid("  ", "pass"));
        assertFalse("Password with spaces", areCredentialsValid("user", "   "));
    }

    @Test
    public void testBoundaryLengths() {
        // 3-character username (minimum valid length)
        assertTrue("Min-length username", areCredentialsValid("abc", "pass"));

        // 25-character username (maximum valid length)
        String maxUser = "a".repeat(25);
        assertTrue("Max-length username", areCredentialsValid(maxUser, "pass"));
    }
}
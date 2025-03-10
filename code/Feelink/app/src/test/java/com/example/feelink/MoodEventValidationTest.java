package com.example.feelink;

import org.junit.Test;
import static org.junit.Assert.*;
import static com.example.feelink.ValidationUtils.*;

public class MoodEventValidationTest {
    // Reason Validation
    @Test
    public void testReasonConstraints() {

        // valid cases
        assertTrue("21 chars", isReasonNotValid("This reason is way too long and exceeds"));
        assertTrue("4 words", isReasonNotValid("one two three four"));

        // InValid cases
        assertFalse(isReasonNotValid("Short reason"));
        assertFalse(isReasonNotValid("Three word limit"));

    }
}
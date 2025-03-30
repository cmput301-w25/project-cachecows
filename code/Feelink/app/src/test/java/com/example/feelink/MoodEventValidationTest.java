package com.example.feelink;

import org.junit.Test;
import static org.junit.Assert.*;
import static com.example.feelink.ValidationUtils.*;

public class MoodEventValidationTest {
    // Reason Validation
    @Test
    public void testReasonConstraints() {
        String invalidReason = new String(new char[201]).replace('\0', 'a');


        // valid case
        assertTrue("201 chars", isReasonNotValid(invalidReason));

        // InValid case
        assertFalse(isReasonNotValid("Short reason"));


    }
}
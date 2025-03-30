package com.example.feelink;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Date;

public class CommentTest {

    @Test
    public void testDefaultConstructor() {
        Comment comment = new Comment();
        // all fields should be null
        assertNull(comment.getId());
        assertNull(comment.getText());
        assertNull(comment.getUserId());
        assertNull(comment.getTimestamp());
        assertNull(comment.getUsername());
    }

    @Test
    public void testParameterizedConstructor() {
        String text = "This is a test comment";
        String userId = "user123";
        Comment comment = new Comment(text, userId);

        assertEquals(text, comment.getText());
        assertEquals(userId, comment.getUserId());

        assertNotNull(comment.getTimestamp());

        long now = new Date().getTime();
        long timestamp = comment.getTimestamp().getTime();
        assertTrue("Timestamp should be recent", now - timestamp < 1000);

        // Username and ID remain unset.
        assertNull( comment.getUsername());
        assertNull(comment.getId());
    }

    @Test
    public void testSettersAndGetters() {
        Comment comment = new Comment();

        comment.setId("comment001");
        comment.setText("Hello, world!");
        comment.setUserId("user456");
        Date now = new Date();
        comment.setTimestamp(now);
        comment.setUsername("Alice");

        assertEquals( "comment001", comment.getId());
        assertEquals("Hello, world!", comment.getText());
        assertEquals("user456", comment.getUserId());
        assertEquals( now, comment.getTimestamp());
        assertEquals("Alice", comment.getUsername());

        assertNotEquals("user123", comment.getUserId());
    }
}

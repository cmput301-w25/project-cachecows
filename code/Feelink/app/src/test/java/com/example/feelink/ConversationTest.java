package com.example.feelink;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class ConversationTest {

    @Test
    public void testDefaultState() {
        Conversation conversation = new Conversation();
        assertNull( conversation.getId());
        assertNull( conversation.getLastMessage());
        assertNull( conversation.getParticipants());
        assertNull(conversation.getTimestamp());
    }

    @Test
    public void testSettersAndGetters() {
        Conversation conversation = new Conversation();
        String expectedId = "conv123";
        String expectedLastMessage = "Hello world!";
        List<String> expectedParticipants = Arrays.asList("user1", "user2");
        Date expectedTimestamp = new Date();

        conversation.setId(expectedId);
        conversation.setLastMessage(expectedLastMessage);
        conversation.setParticipants(expectedParticipants);
        conversation.setTimestamp(expectedTimestamp);

        assertEquals(expectedId, conversation.getId());
        assertEquals(expectedLastMessage, conversation.getLastMessage());
        assertEquals( expectedParticipants, conversation.getParticipants());
        assertEquals(expectedTimestamp, conversation.getTimestamp());
    }

    @Test
    public void testParticipantsContent() {
        Conversation conversation = new Conversation();
        List<String> participants = Arrays.asList("bella", "edward", "jacob");
        conversation.setParticipants(participants);

        List<String> retrievedParticipants = conversation.getParticipants();
        assertNotNull(retrievedParticipants);
        assertEquals(3, retrievedParticipants.size());
        assertTrue( retrievedParticipants.contains("bella"));
        assertTrue(retrievedParticipants.contains("edward"));
        assertTrue(retrievedParticipants.contains("jacob"));
    }

    @Test
    public void testTimestampIsSetCorrectly() {
        Conversation conversation = new Conversation();
        Date now = new Date();
        conversation.setTimestamp(now);

        Date retrievedTimestamp = conversation.getTimestamp();
        assertNotNull(retrievedTimestamp);
        assertEquals(now, retrievedTimestamp);
    }
}

package com.example.feelink;

import org.junit.Test;


import static org.junit.Assert.*;

public class ChatActivityTest {
    @Test
    public void testGenerateConversationId_ordering() {
        // When the IDs are "alice" and "bob", the generated ID should be "alice_bob"
        String convId1 = ChatActivity.generateConversationId("alice", "bob");
        String convId2 = ChatActivity.generateConversationId("bob", "alice");
        assertEquals("alice_bob", convId1);
        assertEquals(convId1, convId2);
    }

    @Test
    public void testGenerateConversationId_emptyInput() {
        // When one ID is empty
        String convId = ChatActivity.generateConversationId("", "bob");
        assertEquals("_bob", convId);
    }

    @Test
    public void testGenerateConversationId_identicalIds() {
        // When both IDs are the same
        String convId = ChatActivity.generateConversationId("alice", "alice");
        assertEquals("alice_alice", convId);
    }
}

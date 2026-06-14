package dev.logicojp.micronaut;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class MessageTest {

    @Test
    void testMessageCreation() {
        Message message = new Message("Hello");
        assertNotNull(message);
        assertEquals("Hello", message.message());
    }

    @Test
    void testMessageEquality() {
        Message message1 = new Message("Hello");
        Message message2 = new Message("Hello");
        assertEquals(message1, message2);
    }

    @Test
    void testMessageInequality() {
        Message message1 = new Message("Hello");
        Message message2 = new Message("World");
        assertNotEquals(message1, message2);
    }

    @Test
    void testMessageToString() {
        Message message = new Message("Hello");
        assertNotNull(message.toString());
    }

    @Test
    void testMessageWithNullContent() {
        Message message = new Message(null);
        assertNotNull(message);
        assertEquals(null, message.message());
    }
}

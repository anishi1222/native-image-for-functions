package dev.logicojp.micronaut;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.Test;

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
        assertEquals(message1.hashCode(), message2.hashCode());
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
        assertEquals("Message[message=Hello]", message.toString());
    }

    @Test
    void testMessageWithNullContent() {
        Message message = new Message(null);
        assertNotNull(message);
        assertNull(message.message());
    }
}

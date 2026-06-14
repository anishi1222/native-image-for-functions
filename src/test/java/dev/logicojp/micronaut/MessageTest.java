package dev.logicojp.micronaut;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MessageTest {

    @Test
    void accessorReturnsConstructorValue() {
        Message message = new Message("hello");
        assertEquals("hello", message.message());
    }

    @Test
    void recordsWithSameValueAreEqualAndShareHashCode() {
        Message first = new Message("same");
        Message second = new Message("same");
        assertEquals(first, second);
        assertEquals(first.hashCode(), second.hashCode());
    }

    @Test
    void recordsWithDifferentValueAreNotEqual() {
        assertNotEquals(new Message("a"), new Message("b"));
    }

    @Test
    void toStringContainsValue() {
        assertTrue(new Message("payload").toString().contains("payload"));
    }

    @Test
    void allowsNullValue() {
        assertNull(new Message(null).message());
    }
}

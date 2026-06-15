package dev.logicojp.micronaut;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;

class GreetingServiceUnitTest {

    private final GreetingService greetingService = new GreetingService();

    @Test
    void greetingUsesProvidedName() {
        Message message = greetingService.greeting("Alice");

        assertNotNull(message);
        assertEquals("Hi, Alice, what's up?", message.message());
    }

    @Test
    void greetingPreservesExactNameText() {
        Message message = greetingService.greeting(" Alice & Bob ");

        assertEquals("Hi,  Alice & Bob , what's up?", message.message());
    }

    @Test
    void greetingHandlesEmptyName() {
        Message message = greetingService.greeting("");

        assertEquals("Hi, , what's up?", message.message());
    }
}
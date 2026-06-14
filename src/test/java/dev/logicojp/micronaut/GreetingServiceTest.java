package dev.logicojp.micronaut;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;

class GreetingServiceTest {

    private final GreetingService service = new GreetingService();

    @Test
    void greetingWithProvidedNameReturnsPersonalizedMessage() {
        Message message = service.greeting("Alice");
        assertEquals("Hi, Alice, what's up?", message.message());
    }

    @Test
    void greetingWithDefaultNameValueReturnsWorldMessage() {
        // "world" is the controller's @QueryValue default; verified here by passing it explicitly
        Message message = service.greeting("world");
        assertEquals("Hi, world, what's up?", message.message());
    }

    @Test
    void greetingWithEmptyNameStillFormatsMessage() {
        Message message = service.greeting("");
        assertEquals("Hi, , what's up?", message.message());
    }

    @Test
    void greetingNeverReturnsNull() {
        assertNotNull(service.greeting("Bob"));
    }
}

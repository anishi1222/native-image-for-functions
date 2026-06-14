package dev.logicojp.micronaut;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@MicronautTest
class GreetingServiceTest {

    @Inject
    @Client("/")
    HttpClient client;

    @Test
    void testGreetingWithDefaultName() {
        Message response = client.toBlocking()
                .retrieve(HttpRequest.GET("/api/greeting"), Message.class);
        assertNotNull(response);
        assertEquals("Hi, world, what's up?", response.message());
    }

    @Test
    void testGreetingWithCustomName() {
        Message response = client.toBlocking()
                .retrieve(HttpRequest.GET("/api/greeting?name=Alice"), Message.class);
        assertNotNull(response);
        assertEquals("Hi, Alice, what's up?", response.message());
    }

    @Test
    void testGreetingResponseIsNotNull() {
        Message response = client.toBlocking()
                .retrieve(HttpRequest.GET("/api/greeting?name=Bob"), Message.class);
        assertNotNull(response);
        assertNotNull(response.message());
    }
}

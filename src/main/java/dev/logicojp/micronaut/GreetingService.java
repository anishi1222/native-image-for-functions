package dev.logicojp.micronaut;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.QueryValue;

import java.util.Map;

@Controller()
public class GreetingService {

    @Get("/greeting")
    public Message greeting(@QueryValue(value = "name", defaultValue = "world") String name) {
        return new Message("Hi, " + name + ", what's up?");
    }
}

package com.gateway.ratelimiter_backend.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class SampleApiController {

    public record User(int id, String name, String email) {}

    public record Product(int id, String name, double price) {}

    @GetMapping("/users")
    public List<User> getUsers() {
        return List.of(
            new User(1, "Aarav", "aarav@example.com"),
            new User(2, "Diya", "diya@example.com"),
            new User(3, "Kabir", "kabir@example.com")
        );
    }

    @GetMapping("/products")
    public List<Product> getProducts() {
        return List.of(
            new Product(1, "Keyboard", 1499.0),
            new Product(2, "Mouse", 799.0),
            new Product(3, "Monitor", 10999.0)
        );
    }
}
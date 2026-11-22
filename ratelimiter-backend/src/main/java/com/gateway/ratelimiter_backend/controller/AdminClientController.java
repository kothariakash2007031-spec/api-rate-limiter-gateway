package com.gateway.ratelimiter_backend.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PutMapping;

import com.gateway.ratelimiter_backend.dto.CreateClientRequest;
import com.gateway.ratelimiter_backend.entity.Client;
import com.gateway.ratelimiter_backend.service.ClientService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/admin/clients")
public class AdminClientController {
	
    @PutMapping("/{id}/plan/{planId}")
    public Client assignPlan(@PathVariable Long id, @PathVariable Long planId) {
        return clientService.assignPlan(id, planId);
    }

    private final ClientService clientService;

    public AdminClientController(ClientService clientService) {
        this.clientService = clientService;
    }

    @PostMapping
    public ResponseEntity<Client> create(@Valid @RequestBody CreateClientRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(clientService.create(request));
    }

    @GetMapping
    public List<Client> list() {
        return clientService.findAll();
    }

    @GetMapping("/{id}")
    public Client get(@PathVariable Long id) {
        return clientService.findById(id);
    }
}
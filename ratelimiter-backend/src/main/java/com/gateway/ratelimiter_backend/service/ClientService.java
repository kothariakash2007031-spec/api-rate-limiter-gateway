package com.gateway.ratelimiter_backend.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.gateway.ratelimiter_backend.dto.CreateClientRequest;
import com.gateway.ratelimiter_backend.entity.Client;
import com.gateway.ratelimiter_backend.entity.Plan;
import com.gateway.ratelimiter_backend.repository.ClientRepository;
import com.gateway.ratelimiter_backend.repository.PlanRepository;

@Service
public class ClientService {

    private final ClientRepository clientRepository;
    private final PlanRepository planRepository;

    public ClientService(ClientRepository clientRepository, PlanRepository planRepository) {
        this.clientRepository = clientRepository;
        this.planRepository = planRepository;
    }

    public Client create(CreateClientRequest request) {
        if (clientRepository.existsByEmail(request.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }
        Client client = new Client();
        client.setName(request.name());
        client.setEmail(request.email());
        return clientRepository.save(client);
    }

    public List<Client> findAll() {
        return clientRepository.findAll();
    }

    public Client findById(Long id) {
        return clientRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Client not found"));
    }

    public Client assignPlan(Long clientId, Long planId) {
        Client client = findById(clientId);
        Plan plan = planRepository.findById(planId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Plan not found"));
        client.setPlan(plan);
        return clientRepository.save(client);
    }
}
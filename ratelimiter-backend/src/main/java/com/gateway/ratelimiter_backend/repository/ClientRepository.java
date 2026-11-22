package com.gateway.ratelimiter_backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.gateway.ratelimiter_backend.entity.Client;

public interface ClientRepository extends JpaRepository<Client, Long> {

    boolean existsByEmail(String email);
}
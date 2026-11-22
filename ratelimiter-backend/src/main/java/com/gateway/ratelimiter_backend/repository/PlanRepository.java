package com.gateway.ratelimiter_backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.gateway.ratelimiter_backend.entity.Plan;

public interface PlanRepository extends JpaRepository<Plan, Long> {

    boolean existsByName(String name);
}
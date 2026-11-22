package com.gateway.ratelimiter_backend.service;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.gateway.ratelimiter_backend.dto.CreatePlanRequest;
import com.gateway.ratelimiter_backend.entity.Plan;
import com.gateway.ratelimiter_backend.repository.PlanRepository;

@Service
public class PlanService {

    private final PlanRepository planRepository;

    public PlanService(PlanRepository planRepository) {
        this.planRepository = planRepository;
    }

    public Plan create(CreatePlanRequest request) {
        if (planRepository.existsByName(request.name())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Plan name already exists");
        }
        Plan plan = new Plan();
        plan.setName(request.name());
        plan.setRequestsPerMinute(request.requestsPerMinute());
        plan.setDailyQuota(request.dailyQuota());
        return planRepository.save(plan);
    }

    public List<Plan> findAll() {
        return planRepository.findAll();
    }
}
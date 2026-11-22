package com.gateway.ratelimiter_backend.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gateway.ratelimiter_backend.dto.CreatePlanRequest;
import com.gateway.ratelimiter_backend.entity.Plan;
import com.gateway.ratelimiter_backend.service.PlanService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/admin/plans")
public class AdminPlanController {

    private final PlanService planService;

    public AdminPlanController(PlanService planService) {
        this.planService = planService;
    }

    @PostMapping
    public ResponseEntity<Plan> create(@Valid @RequestBody CreatePlanRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(planService.create(request));
    }

    @GetMapping
    public List<Plan> list() {
        return planService.findAll();
    }
}
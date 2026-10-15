package com.edu.upeu.PharmaBackend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@io.swagger.v3.oas.annotations.security.SecurityRequirements
@RestController
@RequestMapping({"/api/v1", "/api"})
public class HealthController {
    @GetMapping("/health")
    public String health() {
        return "Backend funcionando correctamente";
    }
}

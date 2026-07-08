package com.supergestor.mercado.controller;

import com.supergestor.mercado.dto.ResumoDashboardResponse;
import com.supergestor.mercado.service.DashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/resumo")
    ResumoDashboardResponse resumo() {
        return dashboardService.resumo();
    }
}


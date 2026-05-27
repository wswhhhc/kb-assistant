package com.example.kbassistant.controller;

import com.example.kbassistant.common.Result;
import com.example.kbassistant.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/statistics")
    public Result<?> statistics() {
        return Result.success(dashboardService.getStatistics());
    }
}

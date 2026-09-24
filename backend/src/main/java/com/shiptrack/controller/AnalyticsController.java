package com.shiptrack.controller;

import com.shiptrack.dto.AnalyticsDtos;
import com.shiptrack.security.UserPrincipal;
import com.shiptrack.service.AnalyticsService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/customer")
    public AnalyticsDtos.CustomerSummary customer(@AuthenticationPrincipal UserPrincipal principal) {
        return analyticsService.customerSummary(principal);
    }

    @GetMapping("/business")
    @PreAuthorize("hasAnyRole('BUSINESS_CLIENT','ADMIN','LOGISTICS_OPERATOR','SUPPORT_AGENT')")
    public AnalyticsDtos.BusinessSummary business(@AuthenticationPrincipal UserPrincipal principal) {
        return analyticsService.businessSummary(principal);
    }

    @GetMapping("/admin")
    @PreAuthorize("hasAnyRole('ADMIN','SUPPORT_AGENT')")
    public AnalyticsDtos.AdminSummary admin() {
        return analyticsService.adminSummary();
    }

    @GetMapping("/routes")
    @PreAuthorize("hasAnyRole('ADMIN','LOGISTICS_OPERATOR','SUPPORT_AGENT')")
    public List<AnalyticsDtos.RoutePerformance> routes() {
        return analyticsService.routePerformance();
    }
}

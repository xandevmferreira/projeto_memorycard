package com.memorycard.controller.web;

import com.memorycard.security.SecurityUtils;
import com.memorycard.service.DashboardService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DashboardWebController {

    private final DashboardService dashboardService;

    public DashboardWebController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        var user = SecurityUtils.getCurrentUser();
        var dashboard = dashboardService.getDashboard(user.getId());

        model.addAttribute("user", user);
        model.addAttribute("dashboard", dashboard);
        return "dashboard/index";
    }
}

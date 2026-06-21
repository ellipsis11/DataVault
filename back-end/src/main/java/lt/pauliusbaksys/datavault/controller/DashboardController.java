package lt.pauliusbaksys.datavault.controller;

import jakarta.servlet.http.HttpServletRequest;
import lt.pauliusbaksys.datavault.dto.AdminDashboard;
import lt.pauliusbaksys.datavault.dto.UserDashboard;
import lt.pauliusbaksys.datavault.service.CurrentUserService;
import lt.pauliusbaksys.datavault.service.DashboardService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;
    private final CurrentUserService currentUserService;

    public DashboardController(DashboardService dashboardService, CurrentUserService currentUserService) {
        this.dashboardService = dashboardService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public Map<String, String> getDashboardPage(){
        return Map.of("message", "access-granted");
    }

    @GetMapping("/user")
    public UserDashboard getDashboardDataForUser(HttpServletRequest req){
        return dashboardService.getDashboardDataForUser(currentUserService.getCurrentUserId(req));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin")
    public AdminDashboard getDashboardDataForAdmin(){
        return dashboardService.getDashboardDataForAdmin();
    }
}
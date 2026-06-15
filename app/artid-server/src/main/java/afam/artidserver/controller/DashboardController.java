package afam.artidserver.controller;

import afam.artidserver.model.dto.DashboardSummaryResponse;
import afam.artidserver.security.AuthenticatedUser;
import afam.artidserver.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/summary")
    public ResponseEntity<DashboardSummaryResponse> summary(@AuthenticationPrincipal AuthenticatedUser principal) {
        return ResponseEntity.ok(dashboardService.summary(principal.getUser()));
    }
}

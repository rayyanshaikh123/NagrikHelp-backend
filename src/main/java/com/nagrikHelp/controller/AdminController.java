package com.nagrikHelp.controller;

import com.nagrikHelp.dto.*;
import com.nagrikHelp.model.IssueStatus;
import com.nagrikHelp.model.User;
import com.nagrikHelp.service.AuthService;
import com.nagrikHelp.service.IssueService;
import com.nagrikHelp.service.NotificationService;
import com.nagrikHelp.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final IssueService issueService;
    private final AuthService authService;
    private final ReportService reportService;
    private final NotificationService notificationService;

    @GetMapping("/ping")
    public ResponseEntity<?> ping(@AuthenticationPrincipal UserDetails user) {
        return ResponseEntity.ok("ADMIN OK: " + (user != null ? user.getUsername() : "unknown"));
    }

    @GetMapping("/issues")
    public List<IssueResponse> getAll(@RequestParam(value = "status", required = false) String status) {
        if (status == null || status.isBlank()) {
            return issueService.getAllIssuesCompat();
        }
        try {
            IssueStatus st = IssueStatus.valueOf(status.trim().toUpperCase().replace('-', '_'));
            return issueService.getIssuesByStatus(st);
        } catch (IllegalArgumentException e) {
            return List.of();
        }
    }

    @PatchMapping("/issues/{id}")
    public ResponseEntity<IssueResponse> update(@PathVariable String id, @RequestBody UpdateIssueRequest req) {
    // Admins editing issues should trigger owner-only notifications for status changes
    return issueService.updateIssueAsAdmin(id, req, null)
        .map(ResponseEntity::ok)
        .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/issues/{id}/status")
    public ResponseEntity<?> updateStatus(@PathVariable String id,
                                          @RequestBody UpdateStatusRequest request,
                                          @AuthenticationPrincipal UserDetails admin) {
        IssueStatus next;
        try {
            next = IssueStatus.valueOf(request.getStatus().trim().toUpperCase().replace('-', '_'));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Invalid status value");
        }
        try {
            return issueService.updateIssueStatus(id, next, admin.getUsername())
                    .<ResponseEntity<?>>map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.notFound().build());
        } catch (IllegalStateException ex) {
            return ResponseEntity.badRequest().body(ex.getMessage());
        }
    }

    @PutMapping("/issues/{id}/assign")
    public ResponseEntity<?> assign(@PathVariable String id,
                                    @RequestBody AssignIssueRequest request,
                                    @AuthenticationPrincipal UserDetails admin) {
        return issueService.assignIssue(id, request.getAssignedTo(), admin.getUsername())
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/admins")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<AdminCreatedResponse> createAdmin(@RequestBody CreateAdminRequest req) {
        User created = authService.createAdmin(req);
        return ResponseEntity.ok(new AdminCreatedResponse(created.getId(), created.getName(), created.getEmail(), created.getRole()));
    }

    @GetMapping("/reports/monthly-resolved")
    public ResponseEntity<MonthlyResolvedReport> monthlyResolved(@RequestParam(value = "year", required = false) Integer year,
                                                                 @RequestParam(value = "month", required = false) Integer month) {
        LocalDate now = LocalDate.now();
        int y = (year == null) ? now.getYear() : year;
        int m = (month == null) ? now.getMonthValue() : month;
        if (m < 1 || m > 12) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(issueService.monthlyResolvedReport(y, m));
    }

    @GetMapping("/reports/monthly-resolved.pdf")
    public ResponseEntity<byte[]> monthlyResolvedPdf(@RequestParam(value = "year", required = false) Integer year,
                                                     @RequestParam(value = "month", required = false) Integer month) {
        LocalDate now = LocalDate.now();
        int y = (year == null) ? now.getYear() : year;
        int m = (month == null) ? now.getMonthValue() : month;
        if (m < 1 || m > 12) {
            return ResponseEntity.badRequest().build();
        }
        byte[] pdf = reportService.generateMonthlyResolvedPdf(y, m);
        String filename = String.format("monthly-issue-report-%d-%02d.pdf", y, m);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(pdf.length)
                .body(pdf);
    }

    @PostMapping("/notifications/test")
    public ResponseEntity<?> testNotification(@RequestParam String email,
                                              @RequestParam(required = false) String issueId,
                                              @RequestParam(required = false) String type,
                                              @RequestParam(required = false) String message) {
        var n = notificationService.createManualNotification(email, issueId, type, message);
        if (n == null) return ResponseEntity.badRequest().body("Failed to create notification");
        return ResponseEntity.ok(Map.of("id", n.getId(), "email", email));
    }
}

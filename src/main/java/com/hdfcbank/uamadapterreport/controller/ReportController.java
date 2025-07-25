package com.hdfcbank.uamadapterreport.controller;

import com.hdfcbank.uamadapterreport.model.ReportRequest;
import com.hdfcbank.uamadapterreport.scheduler.DynamicReportSchedulerService;
import com.hdfcbank.uamadapterreport.service.ReportService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@RestController
@RequestMapping("/api/reports")
@Slf4j
public class ReportController {

    private final ReportService reportService;
    private final DynamicReportSchedulerService dynamicReportSchedulerService;

    public ReportController(ReportService reportService,DynamicReportSchedulerService dynamicReportSchedulerService) {
        this.reportService = reportService;
        this.dynamicReportSchedulerService = dynamicReportSchedulerService;

    }

    @PostMapping("/generate-adhoc")
    public ResponseEntity<String> generateAdhocReport(@RequestBody ReportRequest request) {
        String dateStr = request.getReportDate();
        if (dateStr == null || dateStr.isBlank()) {
            return ResponseEntity.badRequest().body("Report date is required.");
        }

        LocalDate reportDate = parseReportDate(dateStr);
        if (reportDate == null) {
            return ResponseEntity.badRequest().body("Invalid date format. Expected format: yyyy-MM-dd");
        }

        if (reportDate.isAfter(LocalDate.now())) {
            return ResponseEntity.badRequest().body("Report date cannot be in the future.");
        }

        if (!dateStr.matches("\\d{4}-\\d{2}-\\d{2}")) {
            return ResponseEntity.badRequest().body("Date must be in yyyy-MM-dd format.");
        }


        try {
            log.info("Adhoc report generation triggered for date: {}", dateStr);
            reportService.generateAllReportsForDate(dateStr);
            return ResponseEntity.ok("Adhoc reports generated");
        } catch (Exception e) {
            log.error("Error generating adhoc reports: ", e);
            return ResponseEntity.internalServerError().body("Error generating adhoc reports.");
        }
    }

    private LocalDate parseReportDate(String dateStr) {
        try {
            return LocalDate.parse(dateStr, DateTimeFormatter.ISO_DATE);
        } catch (DateTimeParseException e) {
            return null;
        }
    }


    @PostMapping("/reload-schedule")
    public ResponseEntity<String> reloadSchedule() {
        try {
            log.info("Reloading all dynamic report schedules.");
            dynamicReportSchedulerService.reloadSchedules();
            return ResponseEntity.ok("Dynamic report schedules reloaded.");
        } catch (Exception e) {
            log.error("Error reloading schedules: ", e);
            return ResponseEntity.internalServerError().body("Error reloading schedules.");
        }
    }

    @GetMapping("/ready")
    public ResponseEntity<String> readiness() {
        return ResponseEntity.ok("Ready");
    }

    @GetMapping("/healthz")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Healthy");
    }


}

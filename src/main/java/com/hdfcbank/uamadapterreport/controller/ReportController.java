package com.hdfcbank.uamadapterreport.controller;

import com.hdfcbank.uamadapterreport.scheduler.DynamicReportSchedulerService;
import com.hdfcbank.uamadapterreport.service.ReportService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import lombok.extern.slf4j.Slf4j;

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

    @GetMapping("/generate")
    public ResponseEntity<String> generateReport() {
        try {
            log.info("Manual report generation triggered.");
            reportService.generateAllReports();  // <-- Updated method name
            return ResponseEntity.ok("All reports generated and moved successfully.");
        } catch (Exception e) {
            log.error("Error generating reports: ", e);
            return ResponseEntity.internalServerError().body("Error generating reports.");
        }
    }

    @GetMapping("/reload-schedule")
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

}

package com.hdfcbank.uamadapterreport.controller;

import com.hdfcbank.uamadapterreport.scheduler.DynamicReportSchedulerService;
import com.hdfcbank.uamadapterreport.service.ReportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class ReportControllerTest {

    private ReportService reportService;
    private DynamicReportSchedulerService dynamicReportSchedulerService;
    private ReportController reportController;

    @BeforeEach
    void setUp() {
        reportService = mock(ReportService.class);
        dynamicReportSchedulerService = mock(DynamicReportSchedulerService.class);
        reportController = new ReportController(reportService, dynamicReportSchedulerService);
    }

    @Test
    void testGenerateReport_success() {
        ResponseEntity<String> response = reportController.generateReport();

        verify(reportService, times(1)).generateAllReports();
        assertEquals("All reports generated and moved successfully.", response.getBody());
        assertEquals(200, response.getStatusCodeValue());
    }

    @Test
    void testGenerateReport_exception() {
        doThrow(new RuntimeException("Simulated failure")).when(reportService).generateAllReports();

        ResponseEntity<String> response = reportController.generateReport();

        verify(reportService, times(1)).generateAllReports();
        assertEquals("Error generating reports.", response.getBody());
        assertEquals(500, response.getStatusCodeValue());
    }

    @Test
    void testReloadSchedules_success() {
        ResponseEntity<String> response = reportController.reloadSchedule();

        verify(dynamicReportSchedulerService, times(1)).reloadSchedules();
        assertEquals("Dynamic report schedules reloaded.", response.getBody());
        assertEquals(200, response.getStatusCodeValue());
    }

    @Test
    void reloadSchedule_exception() {
        doThrow(new RuntimeException("Simulated schedule reload error")).when(dynamicReportSchedulerService).reloadSchedules();

        ResponseEntity<String> response = reportController.reloadSchedule();

        assertEquals(500, response.getStatusCodeValue());
        assertEquals("Error reloading schedules.", response.getBody());
        verify(dynamicReportSchedulerService, times(1)).reloadSchedules();
    }
}

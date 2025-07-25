package com.hdfcbank.uamadapterreport.controller;

import com.hdfcbank.uamadapterreport.model.ReportRequest;
import com.hdfcbank.uamadapterreport.scheduler.DynamicReportSchedulerService;
import com.hdfcbank.uamadapterreport.service.ReportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;

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
    void testGenerateAdhocReport_success() {
        ReportRequest request = ReportRequest.builder()
                .reportDate("2025-07-24")
                .build();



        String today = LocalDate.now().toString();
        request.setReportDate(today);

        ResponseEntity<String> response = reportController.generateAdhocReport(request);

        verify(reportService, times(1)).generateAllReportsForDate(today);
        assertEquals(200, response.getStatusCodeValue());
        assertEquals("Adhoc reports generated",response.getBody());
    }

    @Test
    void testGenerateAdhocReport_nullDate() {
        ReportRequest request = ReportRequest.builder()
                .reportDate(null)
                .build();


        ResponseEntity<String> response = reportController.generateAdhocReport(request);

        verifyNoInteractions(reportService);
        assertEquals(400, response.getStatusCodeValue());
        assertEquals("Report date is required.", response.getBody());
    }

    @Test
    void testGenerateAdhocReport_blankDate() {
        ReportRequest request = ReportRequest.builder()
                .reportDate("   ")
                .build();


        ResponseEntity<String> response = reportController.generateAdhocReport(request);

        verifyNoInteractions(reportService);
        assertEquals(400, response.getStatusCodeValue());
        assertEquals("Report date is required.", response.getBody());
    }

    @Test
    void testGenerateAdhocReport_invalidDateFormat() {
        ReportRequest request = ReportRequest.builder()
                .reportDate("2025/07/24")
                .build();


        ResponseEntity<String> response = reportController.generateAdhocReport(request);

        verifyNoInteractions(reportService);
        assertEquals(400, response.getStatusCodeValue());
        assertEquals("Invalid date format. Expected format: yyyy-MM-dd", response.getBody());
    }

    @Test
    void testGenerateAdhocReport_futureDate() {
        ReportRequest request = ReportRequest.builder()
                .reportDate(LocalDate.now().plusDays(1).toString())
                .build();


        ResponseEntity<String> response = reportController.generateAdhocReport(request);

        verifyNoInteractions(reportService);
        assertEquals(400, response.getStatusCodeValue());
        assertEquals("Report date cannot be in the future.", response.getBody());
    }

    @Test
    void testGenerateAdhocReport_exception() {

        String today = LocalDate.now().toString();
        ReportRequest request = ReportRequest.builder()
                .reportDate(today)
                .build();


        doThrow(new RuntimeException("Simulated error")).when(reportService).generateAllReportsForDate(today);

        ResponseEntity<String> response = reportController.generateAdhocReport(request);

        assertEquals(500, response.getStatusCodeValue());
        assertEquals("Error generating adhoc reports.", response.getBody());
        verify(reportService, times(1)).generateAllReportsForDate(today);
    }

    @Test
    void testReloadSchedules_success() {
        ResponseEntity<String> response = reportController.reloadSchedule();

        verify(dynamicReportSchedulerService, times(1)).reloadSchedules();
        assertEquals(200, response.getStatusCodeValue());
        assertEquals("Dynamic report schedules reloaded.", response.getBody());
    }

    @Test
    void testReloadSchedule_exception() {
        doThrow(new RuntimeException("Simulated error")).when(dynamicReportSchedulerService).reloadSchedules();

        ResponseEntity<String> response = reportController.reloadSchedule();

        verify(dynamicReportSchedulerService, times(1)).reloadSchedules();
        assertEquals(500, response.getStatusCodeValue());
        assertEquals("Error reloading schedules.", response.getBody());
    }

    @Test
    void testReadinessEndpoint() {
        ResponseEntity<String> response = reportController.readiness();

        assertEquals(200, response.getStatusCodeValue());
        assertEquals("Ready", response.getBody());
    }

    @Test
    void testHealthEndpoint() {
        ResponseEntity<String> response = reportController.health();

        assertEquals(200, response.getStatusCodeValue());
        assertEquals("Healthy", response.getBody());
    }

    @Test
    void testGenerateAdhocReport_invalidDate_missingZeroPadding() {
        ReportRequest request = ReportRequest.builder()
                .reportDate("2025-7-4")  // not yyyy-MM-dd
                .build();

        ResponseEntity<String> response = reportController.generateAdhocReport(request);

        verifyNoInteractions(reportService);
        assertEquals(400, response.getStatusCodeValue());
        assertEquals("Invalid date format. Expected format: yyyy-MM-dd", response.getBody());
    }

    @Test
    void testGenerateAdhocReport_invalidDate_extraCharacters() {
        ReportRequest request = ReportRequest.builder()
                .reportDate("2025-07-24abc")  // extra characters
                .build();

        ResponseEntity<String> response = reportController.generateAdhocReport(request);

        verifyNoInteractions(reportService);
        assertEquals(400, response.getStatusCodeValue());
        assertEquals("Invalid date format. Expected format: yyyy-MM-dd", response.getBody());
    }


}

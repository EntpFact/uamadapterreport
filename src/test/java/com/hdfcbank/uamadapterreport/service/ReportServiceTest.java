package com.hdfcbank.uamadapterreport.service;

import com.hdfcbank.uamadapterreport.model.ReportConfig;
import com.hdfcbank.uamadapterreport.repository.ReportConfigRepository;
import com.hdfcbank.uamadapterreport.util.TestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ReportServiceTest {

    private ReportConfigRepository configRepo;
    private DynamicReportQueryService queryService;
    private CSVReportGenerator csvReportGenerator;
    private SFTPService sftpService;
    private ReportService reportService;
    private EmailService emailService;

    private final String reportDir = System.getProperty("java.io.tmpdir");

    @BeforeEach
    void setUp() {
        configRepo = mock(ReportConfigRepository.class);
        queryService = mock(DynamicReportQueryService.class);
        csvReportGenerator = mock(CSVReportGenerator.class);
        sftpService = mock(SFTPService.class);
        emailService = mock(EmailService.class);

        reportService = new ReportService(configRepo, queryService, csvReportGenerator, sftpService,emailService);
        TestUtils.setField(reportService, "reportDirectory", reportDir);
        TestUtils.setField(reportService, "defaultSftpDirectory", "/upload");
        TestUtils.setField(reportService, "retryCount", 2);
        TestUtils.setField(reportService, "retryDelayMs", 10L);
    }

    @Test
    void testGenerateAllReports_withValidData() throws Exception {
        String pattern = "report_{timestamp}.csv";
        ReportConfig config = createReportConfig("Report1", pattern, "/upload/custom");

        when(configRepo.findByActiveTrue()).thenReturn(List.of(config));
        when(queryService.runQuery(anyString())).thenReturn(List.of(Map.of("id", 1)));

        doAnswer(invocation -> {
            String pathStr = invocation.getArgument(1);
            Files.write(Paths.get(pathStr), "id\n1".getBytes());
            return null;
        }).when(csvReportGenerator).generateCSVReport(anyList(), anyString());

        reportService.generateAllReports();

        verify(sftpService).uploadFile(
                argThat(localPath -> localPath.endsWith(".csv")),
                argThat(remotePath -> remotePath.startsWith("/upload/custom/report_") && remotePath.endsWith(".csv"))
        );
    }

    @Test
    void testGenerateAllReports_missingFile_shouldSkipUploadAndLogError() throws Exception {
        ReportConfig config = createReportConfig("Missing", "missing_{timestamp}.csv", null);
        when(configRepo.findByActiveTrue()).thenReturn(List.of(config));
        when(queryService.runQuery(anyString())).thenReturn(List.of());

        doNothing().when(csvReportGenerator).generateCSVReport(anyList(), anyString());

        assertDoesNotThrow(() -> reportService.generateAllReports());

        verify(sftpService, never()).uploadFile(any(), any());
    }

    @Test
    void testGenerateSingleReport_success() throws Exception {
        String pattern = "single_{timestamp}.csv";
        ReportConfig config = createReportConfig("Single", pattern, "/upload/single");

        when(queryService.runQuery(anyString())).thenReturn(List.of(Map.of("key", "value")));

        doAnswer(invocation -> {
            String pathStr = invocation.getArgument(1);
            Files.write(Paths.get(pathStr), "key,value".getBytes());
            return null;
        }).when(csvReportGenerator).generateCSVReport(anyList(), anyString());

        reportService.generateReport(config);

        verify(sftpService).uploadFile(
                argThat(localPath -> localPath.endsWith(".csv")),
                argThat(remotePath -> remotePath.startsWith("/upload/single/single_") && remotePath.endsWith(".csv"))
        );
    }

    @Test
    void testGenerateReport_withInvalidReportDirectory_shouldLogErrorAndNotUpload() {
        ReportConfig config = new ReportConfig();
        config.setReportName("Invalid Directory Report");
        config.setSqlQuery("SELECT 1");
        config.setFileNamePattern("bad_report_{timestamp}.csv");

        // Set invalid reportDirectory to blank string to trigger validation failure
        ReflectionTestUtils.setField(reportService, "reportDirectory", " ");

        // Call generateReport - no exception should be thrown because it's caught inside
        assertDoesNotThrow(() -> reportService.generateReport(config));

        // Verify uploadFile was never called due to failure
        verify(sftpService, never()).uploadFile(anyString(), anyString());
    }

    private ReportConfig createReportConfig(String name, String filePattern, String sftpPath) {
        ReportConfig config = new ReportConfig();
        config.setReportName(name);
        config.setSqlQuery("SELECT 1");
        config.setFileNamePattern(filePattern);
        config.setSftpTargetPath(sftpPath);
        config.setActive(true);
        return config;
    }

    @Test
    void testGenerateAllReportsForDate_success() throws Exception {
        String reportDate = "2025-07-24";
        String pattern = "adhoc_{timestamp}.csv";
        ReportConfig config = createReportConfig("AdhocReport", pattern, "/upload/adhoc");

        when(configRepo.findByActiveTrue()).thenReturn(List.of(config));
        when(queryService.runQuery(anyString())).thenReturn(List.of(Map.of("id", 123)));

        doAnswer(invocation -> {
            String pathStr = invocation.getArgument(1);
            Files.write(Paths.get(pathStr), "id\n123".getBytes());
            return null;
        }).when(csvReportGenerator).generateCSVReport(anyList(), anyString());

        reportService.generateAllReportsForDate(reportDate);

        verify(sftpService).uploadFile(
                argThat(localPath -> localPath.endsWith(".csv")),
                argThat(remotePath -> remotePath.startsWith("/upload/adhoc/adhoc_") && remotePath.endsWith(".csv"))
        );
    }

    @Test
    void testGenerateAllReportsForDate_emptyQueryResult() throws Exception {
        String reportDate = "2025-07-24";
        ReportConfig config = createReportConfig("EmptyQuery", "empty_{timestamp}.csv", null);

        when(configRepo.findByActiveTrue()).thenReturn(List.of(config));
        when(queryService.runQuery(anyString())).thenReturn(List.of()); // empty result

        doAnswer(invocation -> {
            String pathStr = invocation.getArgument(1);
            Files.write(Paths.get(pathStr), "".getBytes());
            return null;
        }).when(csvReportGenerator).generateCSVReport(anyList(), anyString());

        reportService.generateAllReportsForDate(reportDate);

        verify(sftpService).uploadFile(anyString(), anyString());
    }
    @Test
    void testGenerateAllReportsForDate_invalidFilePattern() {
        String reportDate = "2025-07-24";
        ReportConfig config = createReportConfig("BadPattern", "no_timestamp.csv", "/upload/bad");

        when(configRepo.findByActiveTrue()).thenReturn(List.of(config));

        assertDoesNotThrow(() -> reportService.generateAllReportsForDate(reportDate));

        verify(sftpService, never()).uploadFile(anyString(), anyString());
    }

}

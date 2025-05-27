package com.hdfcbank.uamadapterreport.service;

import com.hdfcbank.uamadapterreport.exception.CustomException;
import com.hdfcbank.uamadapterreport.model.ReportConfig;
import com.hdfcbank.uamadapterreport.repository.ReportConfigRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class ReportService {

    private final ReportConfigRepository configRepo;
    private final DynamicReportQueryService queryService;
    private final CSVReportGenerator csvReportGenerator;
    private final SFTPService sftpService;

    @Value("${report.directory}")
    private String reportDirectory;

    @Value("${sftp.directory}")
    private String defaultSftpDirectory;

    @Value("${report.cleanup.retry-count:3}")  // default to 3 if not set
    private int retryCount;

    @Value("${report.cleanup.retry-delay-ms:500}")  // default to 500ms
    private long retryDelayMs;

    // Lock map to track running reports by report name
    private final ConcurrentHashMap<String, Object> runningReports = new ConcurrentHashMap<>();

    public ReportService(ReportConfigRepository configRepo,
                         DynamicReportQueryService queryService,
                         CSVReportGenerator csvReportGenerator,
                         SFTPService sftpService) {
        this.configRepo = configRepo;
        this.queryService = queryService;
        this.csvReportGenerator = csvReportGenerator;
        this.sftpService = sftpService;
    }

    /**
     * Generate all active reports sequentially.
     * Does not allow parallel execution of the same report.
     */
    public void generateAllReports() {
        log.info("START generateAllReports - Thread: {} - Timestamp: {}", Thread.currentThread().getName(), System.currentTimeMillis());
        List<ReportConfig> configs = configRepo.findByActiveTrue();

        for (ReportConfig config : configs) {
            generateReport(config);
        }
        log.info("Finished generateAllReports - Thread: {}", Thread.currentThread().getName());
    }

    /**
     * Generate a single report with locking to avoid parallel runs of the same report.
     */
    public void generateReport(ReportConfig config) {
        String reportName = config.getReportName();
        Object lock = new Object();

        // Try to acquire lock for this report
        Object existingLock = runningReports.putIfAbsent(reportName, lock);
        if (existingLock != null) {
            log.warn("Report '{}' is already running. Skipping concurrent execution.", reportName);
            return;
        }

        log.info("START generateReport for {} - Thread: {} - Timestamp: {}", reportName, Thread.currentThread().getName(), System.currentTimeMillis());

        try {
            validateReportDirectory();

            String fileName = generateFileName(config);
            Path filePath = Paths.get(reportDirectory, fileName);

            log.info("Generating report: {} with file name: {}", reportName, fileName);

            List<Map<String, Object>> data = queryService.runQuery(config.getSqlQuery());
            generateCSVReport(data, filePath);

            validateReportFileExists(filePath);

            String sftpDir = getSftpDirectory(config);
            uploadAndCleanupReport(filePath, fileName, sftpDir);

        } catch (CustomException e) {
            log.error("Custom exception during report generation for {}: {}", reportName, e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error generating report for {}", reportName, e);
        } finally {
            // Release lock
            runningReports.remove(reportName);
            log.info("Finished generateReport for {} - Thread: {}", reportName, Thread.currentThread().getName());
        }
    }

    private void validateReportDirectory() {
        if (reportDirectory == null || reportDirectory.trim().isEmpty()) {
            throw new CustomException("Report directory is not specified or invalid.");
        }
    }

    private String generateFileName(ReportConfig config) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String pattern = config.getFileNamePattern();

        if (pattern == null || pattern.trim().isEmpty()) {
            throw new CustomException("File name pattern is missing in config: " + config.getReportName());
        }

        return pattern.replace("{timestamp}", timestamp);
    }

    private void generateCSVReport(List<Map<String, Object>> data, Path filePath) throws IOException {
        csvReportGenerator.generateCSVReport(data.isEmpty() ? List.of() : data, filePath.toString());
        log.info("CSV report generated: {}", filePath);
    }

    private void validateReportFileExists(Path filePath) {
        if (!Files.exists(filePath)) {
            throw new CustomException("CSV report file missing: " + filePath);
        }
    }

    private String getSftpDirectory(ReportConfig config) {
        return (config.getSftpTargetPath() != null && !config.getSftpTargetPath().trim().isEmpty())
                ? config.getSftpTargetPath()
                : defaultSftpDirectory;
    }

    private void uploadAndCleanupReport(Path filePath, String fileName, String sftpDirectory) {
        try {
            sftpService.uploadFile(filePath.toString(), sftpDirectory + "/" + fileName);
            log.info("Report uploaded to SFTP: {}", fileName);

            deleteFileWithRetries(filePath);

        } catch (Exception e) {
            throw new CustomException("Failed to upload/delete report: " + fileName, e);
        }
    }

    private void deleteFileWithRetries(Path filePath) {
        int attempts = 0;
        boolean deleted = false;

        while (attempts < retryCount && !deleted) {
            try {
                Files.deleteIfExists(filePath);
                deleted = true;
                log.info("Local report file deleted: {}", filePath);
            } catch (IOException ex) {
                attempts++;
                log.warn("Attempt {} to delete file failed: {}. Retrying...", attempts, filePath, ex);

                try {
                    Thread.sleep(retryDelayMs); // Only this throws InterruptedException
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt(); // good practice
                    log.error("Thread interrupted during retry delay for file deletion: {}", filePath, ie);
                    return;
                }
            }
        }

        if (!deleted) {
            log.error("Failed to delete local report file after {} attempts: {}", attempts, filePath);
        }
    }

}




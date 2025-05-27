package com.hdfcbank.uamadapterreport.scheduler;

import com.hdfcbank.uamadapterreport.model.ReportConfig;
import com.hdfcbank.uamadapterreport.repository.ReportConfigRepository;
import com.hdfcbank.uamadapterreport.service.ReportService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ScheduledFuture;

@Slf4j
@Service
public class DynamicReportSchedulerService {

    private final ReportConfigRepository configRepository;
    private final ReportService reportService;

    private final TaskScheduler taskScheduler;
    private final Map<Long, ScheduledFuture<?>> scheduledTasks = new HashMap<>();

    public DynamicReportSchedulerService(ReportConfigRepository configRepository,
                                         ReportService reportService) {
        this.configRepository = configRepository;
        this.reportService = reportService;

        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(10);
        scheduler.setThreadNamePrefix("report-scheduler-");
        scheduler.initialize();
        this.taskScheduler = scheduler;
    }

    @PostConstruct
    public void initialize() {
        scheduleAllReports();
    }

    public void scheduleAllReports() {
        cancelAllScheduledReports(); // Reschedule in case configs are updated
        List<ReportConfig> configs = configRepository.findByActiveTrue();

        for (ReportConfig config : configs) {
            if (config.getScheduleCron() == null || config.getScheduleCron().isBlank()) {
                log.warn("No cron expression for report: {}", config.getReportName());
                continue;
            }

            scheduleReport(config);
        }
    }

    public void scheduleReport(ReportConfig config) {
        Runnable task = () -> {
            try {
                log.info("Running scheduled task for report: {}", config.getReportName());
                reportService.generateReport(config);
            } catch (Exception e) {
                log.error("Failed to generate report for: {}", config.getReportName(), e);
            }
        };

        Trigger trigger = new CronTrigger(config.getScheduleCron());
        ScheduledFuture<?> future = taskScheduler.schedule(task, trigger);
        scheduledTasks.put(config.getId(), future);

        log.info("Scheduled report: {} with cron: {}", config.getReportName(), config.getScheduleCron());
    }

    public void cancelAllScheduledReports() {
        for (ScheduledFuture<?> future : scheduledTasks.values()) {
            if (future != null) {
                future.cancel(false);
            }
        }
        scheduledTasks.clear();
    }

    public void reloadSchedules() {
        log.info("Reloading dynamic report schedules...");
        scheduleAllReports();
    }
}

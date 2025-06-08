package com.hdfcbank.uamadapterreport.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
public class ReportConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String applicationName;

    private String applicationId;

    private String reportName;

    private String fileNamePattern; // e.g., report_user_{timestamp}.xlsx

    @Column(columnDefinition = "TEXT")
    private String sqlQuery;

    private String scheduleCron; // Optional if needed for per-report scheduling

    private boolean active;

    @Column(name = "sftp_target_path")
    private String sftpTargetPath;  // New field to store per-report SFTP directory
}

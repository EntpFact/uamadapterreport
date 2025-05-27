package com.hdfcbank.uamadapterreport;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.hdfcbank.uamadapterreport")
@EnableScheduling
public class UamAdapterReportGenerationApplication {
    public static void main(String[] args) {
        SpringApplication.run(UamAdapterReportGenerationApplication.class, args);
    }
}
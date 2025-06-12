package com.hdfcbank.uamadapterreport;

import com.hdfcbank.uamadapterreport.config.CustomizePropertiesListener;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.hdfcbank.uamadapterreport")
@EnableScheduling
public class UamAdapterReportGenerationApplication {
    public static void main(String[] args) {
        new SpringApplicationBuilder(UamAdapterReportGenerationApplication.class)
                .listeners(new CustomizePropertiesListener())
                .run(args);
    }
}
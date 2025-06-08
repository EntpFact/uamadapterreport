package com.hdfcbank.uamadapterreport.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Getter
@Setter
@Configuration
@ConfigurationProperties
public class MultiPathProperties {

    private Map<String, AppEmailConfig> applications;

    @Getter
    @Setter
    public static class AppEmailConfig {
        private EmailProps success;

        @Getter
        @Setter
        public static class EmailProps {
            private String to;
            private String from;
            private String cc;
            private String bcc;
            private String subject;
            private String status;
            private String reachOutEmail;
        }

    }
}

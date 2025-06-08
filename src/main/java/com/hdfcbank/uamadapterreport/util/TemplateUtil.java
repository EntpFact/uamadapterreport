package com.hdfcbank.uamadapterreport.util;

import com.hdfcbank.uamadapterreport.exception.CustomException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Slf4j
public class TemplateUtil {

    /**
     * Template key format for environment properties:
     * e.g. template.rupay.rupay-success-email-template
     */
    public static final String TEMPLATE_FORMAT = "template.%s.%s-%s-email-template";

    private TemplateUtil() {
        // Private constructor to prevent instantiation of utility class
    }

    /**
     * Reads template content from environment properties based on networkType and operationStatus,
     * then replaces placeholders with provided values.
     *
     * @param env              Spring Environment to read properties
     * @param appName      e.g. "rupay"
     * @param operationStatus  e.g. "success" or "error"
     *
     * @param applicationId
     * @param sftpPath
     * @param fileNames
     * @param reachOutEmail
     * @return processed email body string
     */
    public static String getEmailBodyFromEnvTemplate(
            Environment env,
            String appName,
            String operationStatus,
            String applicationId,
            String sftpPath,
            List<String> fileNames,
            String reachOutEmail) {

        try {
            // Construct property key: e.g. "template.rupay.rupay-success-email-template"
            String templateKey = String.format(
                    TEMPLATE_FORMAT,
                    appName.toLowerCase(Locale.ROOT),
                    appName.toLowerCase(Locale.ROOT),
                    operationStatus.toLowerCase(Locale.ROOT)
            );

            // Security check for property key format (optional but recommended)
            if (!templateKey.matches("^template\\.[a-zA-Z0-9]+\\.[a-zA-Z0-9]+-[a-zA-Z0-9]+-email-template$")) {
                throw new CustomException("Invalid template property key format");
            }

            // Read template content from environment
            String templateContent = env.getProperty(templateKey);
            if (templateContent == null || templateContent.isBlank()) {
                throw new CustomException("Template content not found or empty for key: " + templateKey);
            }

            // Prepare formatted list of files as a string with newlines
            StringBuilder reportFilesFormatted = new StringBuilder();
            for (String file : fileNames) {
                reportFilesFormatted.append(file).append("\n");
            }

            // Replace placeholders
            templateContent = templateContent.replace("<applicationId>", applicationId);
            templateContent = templateContent.replace("<applicationName>", appName);
            templateContent = templateContent.replace("<date>", LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            templateContent = templateContent.replace("<sftpPath>", sftpPath);
            templateContent = templateContent.replace("<reportFilesFormatted>", reportFilesFormatted.toString().trim());
            templateContent = templateContent.replace("<reachOutEmail>", reachOutEmail);

            return templateContent;

        } catch (Exception e) {
            log.error("Failed to load email template from environment property: {}", e.getMessage(), e);
            throw new CustomException("Error loading email template from environment", e);
        }
    }
}

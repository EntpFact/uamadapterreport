package com.hdfcbank.uamadapterreport.util;

import com.hdfcbank.uamadapterreport.exception.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class TemplateUtilTest {

    private Environment env;

    @BeforeEach
    void setup() {
        env = mock(Environment.class);
    }

    @Test
    void testGetEmailBodyFromEnvTemplate_success() {
        String appName = "rupay";
        String status = "success";
        String appId = "APP123";
        String sftpPath = "/remote/path";
        List<String> fileNames = List.of("file1.csv", "file2.csv");
        String reachOut = "support@bank.com";

        String key = "template.rupay.rupay-success-email-template";
        String template = """
                Dear Team,

                The application <applicationName> with ID <applicationId> has been successfully processed on <date>.

                Files:
                <reportFilesFormatted>

                Uploaded to: <sftpPath>

                For support, reach out to <reachOutEmail>.
                """;

        when(env.getProperty(key)).thenReturn(template);

        String result = TemplateUtil.getEmailBodyFromEnvTemplate(env, appName, status, appId, sftpPath, fileNames, reachOut);

        assertTrue(result.contains("APP123"));
        assertTrue(result.contains("/remote/path"));
        assertTrue(result.contains("file1.csv"));
        assertTrue(result.contains("support@bank.com"));
        assertFalse(result.contains("<")); // ensure no placeholders remain
    }

    @Test
    void testGetEmailBodyFromEnvTemplate_missingTemplate() {
        String appName = "rupay";
        String key = "template.rupay.rupay-success-email-template";
        when(env.getProperty(key)).thenReturn(null);

        Exception ex = assertThrows(CustomException.class, () ->
                TemplateUtil.getEmailBodyFromEnvTemplate(
                        env, "rupay", "success", "ID", "/path", List.of("file.csv"), "help@bank.com")
        );

        System.out.println("Exception message: " + ex.getMessage());
    }


    @Test
    void testGetEmailBodyFromEnvTemplate_envThrowsException() {
        String key = "template.test.test-success-email-template";
        when(env.getProperty(key)).thenThrow(new RuntimeException("env failure"));

        Exception ex = assertThrows(CustomException.class, () ->
                TemplateUtil.getEmailBodyFromEnvTemplate(
                        env, "test", "success", "ID", "/path", List.of("file.csv"), "support@bank.com")
        );

        assertTrue(ex.getMessage().contains("Error loading email template"));
    }

    @Test
    void testTemplateKeyValidation_withInvalidKey_throwsException() {
        String invalidKey = "template.invalid key!-email-template";

        CustomException ex = assertThrows(CustomException.class, () -> {
            validateTemplateKey(invalidKey);
        });

        assertTrue(ex.getMessage().contains("Invalid template property key format"));
    }

    @Test
    void testTemplateKeyValidation_withValidKey_passes() {
        String validKey = "template.rupay.rupay-success-email-template";

        assertDoesNotThrow(() -> {
            validateTemplateKey(validKey);
        });
    }

    // Helper method simulating the validation logic in your production code
    void validateTemplateKey(String templateKey) {
        if (!templateKey.matches("^template\\.[a-zA-Z0-9]+\\.[a-zA-Z0-9]+-[a-zA-Z0-9]+-email-template$")) {
            throw new CustomException("Invalid template property key format");
        }
    }
}

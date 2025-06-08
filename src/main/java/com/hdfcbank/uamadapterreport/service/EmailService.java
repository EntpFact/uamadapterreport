package com.hdfcbank.uamadapterreport.service;

import com.hdfcbank.uamadapterreport.config.MultiPathProperties;
import com.hdfcbank.uamadapterreport.config.MultiPathProperties.AppEmailConfig;
import com.hdfcbank.uamadapterreport.exception.CustomException;
import com.hdfcbank.uamadapterreport.model.Response;
import com.hdfcbank.uamadapterreport.util.TemplateUtil;
import com.hdfcbank.ef.apiconnect.builder.PostRequest;
import com.hdfcbank.ef.apiconnect.service.APIClient;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static com.hdfcbank.uamadapterreport.util.Constant.SUCCESS;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final Environment env;
    private final MultiPathProperties multiPathProperties;
    private final APIClient apiClient;

    @Value("${email.rest.url}")
    private String emailRestURL;

    public void sendEmail(Map<String, Object> headers) {
        try {
            log.info("Received headers map for email: {}", headers);

            String status = getString(headers, "status", SUCCESS);
            String applicationName = getString(headers, "applicationName", null);
            String applicationId = getString(headers, "applicationId", null);
            String sftpPath = getString(headers, "sftpPath", null);

            List<String> fileNames = null;
            Object fileNamesObj = headers.get("fileNames");
            if (fileNamesObj instanceof List<?> list) {
                fileNames = list.stream()
                        .filter(String.class::isInstance)
                        .map(String.class::cast)
                        .toList();
            }

            if (applicationName == null || applicationId == null) {
                throw new CustomException("Missing required email parameters: applicationName or applicationId");
            }

            sendEmail(status, applicationName, applicationId, sftpPath, fileNames);

        } catch (Exception e) {
            log.error("Error in sendEmail(Map) method", e);
            throw e;
        }
    }

    public void sendEmail(String status, String applicationName, String applicationId, String sftpPath, List<String> fileNames) {
        log.info("EmailService call initiated");

        try {
            if (!SUCCESS.equalsIgnoreCase(status)) {
                log.warn("Only success status supported. Ignored for status: {}", status);
                return;
            }

            AppEmailConfig appConfig = multiPathProperties.getApplications().get(applicationName.toLowerCase());
            if (appConfig == null || appConfig.getSuccess() == null ) {
                throw new CustomException("Missing email config for application: " + applicationName);
            }

            var config = appConfig.getSuccess();

            String body = TemplateUtil.getEmailBodyFromEnvTemplate(
                    env,
                    applicationName,
                    SUCCESS,
                    applicationId,
                    sftpPath,
                    fileNames,
                    config.getReachOutEmail()
            );
            log.info("Sending email body: {}", body);

            Map<String, Object> emailPayload = Map.of(
                    "to", config.getTo(),
                    "from", config.getFrom(),
                    "cc", config.getCc(),
                    "bcc", config.getBcc(),
                    "subject", config.getSubject() + " - " + applicationName,
                    "mailBody", body,
                    "status", SUCCESS,
                    "reconId", applicationId
            );

            PostRequest request = PostRequest.builder()
                    .url(emailRestURL)
                    .body(emailPayload)
                    .responseType(Response.class)
                    .headers(headers -> headers.add("Authorization", "Bearer token"))
                    .build();

            Object result = apiClient.execute(request);

            if (result instanceof Mono<?> mono) {
                @SuppressWarnings("unchecked")
                Mono<Response> responseMono = (Mono<Response>) mono;

                Response response = responseMono
                        .retryWhen(Retry.backoff(1, Duration.ofSeconds(5)))
                        .block();

                if (response != null) {
                    String statusResp = response.getStatus();
                    String message = response.getMessage();
                    log.info("EmailService response status: {}, message: {}", statusResp, message);

                    if (!SUCCESS.equalsIgnoreCase(statusResp)) {
                        throw new CustomException("EmailService responded with failure: " + message);
                    }
                } else {
                    throw new CustomException("EmailService returned empty response");
                }

            } else {
                throw new CustomException("Expected Mono<Response> from apiClient");
            }

            log.info("EmailService call completed successfully");

        } catch (Exception e) {
            log.error("Error while sending email: {}", e.getMessage(), e);
            throw new CustomException("Failed to send email: " + e.getMessage(), e);
        }
    }

    private String getString(Map<String, Object> map, String key, String defaultVal) {
        Object val = map.get(key);
        if (val instanceof String strVal) {
            return strVal;
        }
        return defaultVal;
    }
}


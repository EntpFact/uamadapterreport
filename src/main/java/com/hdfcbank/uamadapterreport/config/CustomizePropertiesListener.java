package com.hdfcbank.uamadapterreport.config;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import com.hdfcbank.uamadapterreport.exception.CustomException;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertiesPropertySource;
import io.dapr.client.DaprClient;
import io.dapr.client.DaprClientBuilder;
import lombok.extern.slf4j.Slf4j;

/**
 *
 */
@Slf4j
public class CustomizePropertiesListener implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {

    @Override
    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
        ConfigurableEnvironment environment = event.getEnvironment();
        Properties props = new Properties();
        Binder binder = Binder.get(environment);

        String componentName = environment.getProperty("secret.componentName");
        if (componentName == null || componentName.isBlank()) {
            log.warn("Secret componentName is not configured. Skipping secret loading.");
            return;
        }

        Map<String, String> secretMap = binder
                .bind("secret.map", Bindable.mapOf(String.class, String.class))
                .orElseThrow(() -> new RuntimeException("Secret map not found in configuration."));

        secretMap.forEach((k, v) -> {
            if (v == null || v.isBlank()) {
                log.warn("Secret mapping for [{}] is blank. Skipping fetch and injecting empty string.", k);
                props.put(k, "");
            } else {
                props.put(k, getPreprtyValue(componentName, v));
            }
        });

        environment.getPropertySources().addFirst(new PropertiesPropertySource("props", props));
    }

    private String getPreprtyValue(String componentName, String secretName) {
        try (DaprClient daprClient = new DaprClientBuilder().build()) {
            log.info("Fetching componentName {} and secretName:{}", componentName, secretName);
            Map<String, String> secretData = daprClient.getSecret(componentName, secretName).share().block();
            return Optional.ofNullable(secretData.get(secretName))
                    .orElseThrow(() -> new CustomException("Secret key not found: " + secretName));

        } catch (Exception e) {
            log.error("Error while fetching secret value:{}", e.getMessage());
            throw new CustomException("Exception while fetching secret value", e);
        }
    }

}

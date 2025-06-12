package com.hdfcbank.uamadapterreport.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.Map;

import com.hdfcbank.uamadapterreport.exception.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.boot.context.properties.bind.BindResult;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertiesPropertySource;


import io.dapr.client.DaprClient;
import io.dapr.client.DaprClientBuilder;
import reactor.core.publisher.Mono;

class CustomizePropertiesListenerTest {

    private CustomizePropertiesListener listener;

    @BeforeEach
    void setUp() {
        listener = new CustomizePropertiesListener();
    }

    @Test
    void testOnApplicationEvent_success() {
        ConfigurableEnvironment environment = mock(ConfigurableEnvironment.class);
        ApplicationEnvironmentPreparedEvent event = mock(ApplicationEnvironmentPreparedEvent.class);
        when(event.getEnvironment()).thenReturn(environment);

        when(environment.getProperty("secret.componentName")).thenReturn("testComponent");

        MutablePropertySources propertySources = new MutablePropertySources();
        when(environment.getPropertySources()).thenReturn(propertySources);

        try (MockedStatic<Binder> binderStatic = mockStatic(Binder.class)) {
            Binder binder = mock(Binder.class);
            binderStatic.when(() -> Binder.get(environment)).thenReturn(binder);
            BindResult<Map<String, String>> mockBindResult = mock(BindResult.class);
            when(mockBindResult.orElseThrow(any())).thenReturn(Map.of("some.key", "secretName"));
            when(binder.bind(eq("secret.map"), any(Bindable.class))).thenReturn(mockBindResult);

            try (MockedConstruction<DaprClientBuilder> builderMock = mockConstruction(DaprClientBuilder.class,
                    (builder, context) -> {
                        DaprClient client = mock(DaprClient.class);
                        when(client.getSecret("testComponent", "secretName"))
                                .thenReturn(Mono.just(Map.of("secretName", "secretValue")));
                        when(builder.build()).thenReturn(client);
                    })) {

                listener.onApplicationEvent(event);

                PropertiesPropertySource props = (PropertiesPropertySource) propertySources.get("props");
                assertNotNull(props);
                assertEquals("secretValue", props.getProperty("some.key"));
            }
        }
    }

    @Test
    void testOnApplicationEvent_daprFailure() {
        ConfigurableEnvironment environment = mock(ConfigurableEnvironment.class);
        ApplicationEnvironmentPreparedEvent event = mock(ApplicationEnvironmentPreparedEvent.class);
        when(event.getEnvironment()).thenReturn(environment);
        when(environment.getProperty("secret.componentName")).thenReturn("badComponent");

        MutablePropertySources propertySources = new MutablePropertySources();
        when(environment.getPropertySources()).thenReturn(propertySources);

        try (MockedStatic<Binder> binderStatic = mockStatic(Binder.class)) {
            Binder binder = mock(Binder.class);
            binderStatic.when(() -> Binder.get(environment)).thenReturn(binder);
            BindResult<Map<String, String>> mockBindResult = mock(BindResult.class);
            when(mockBindResult.orElseThrow(any())).thenReturn(Map.of("some.key", "badSecret"));
            when(binder.bind(eq("secret.map"), any(Bindable.class))).thenReturn(mockBindResult);

            try (MockedConstruction<DaprClientBuilder> builderMock = mockConstruction(DaprClientBuilder.class,
                    (builder, context) -> {
                        DaprClient client = mock(DaprClient.class);
                        when(client.getSecret("badComponent", "badSecret"))
                                .thenThrow(new RuntimeException("Dapr error"));
                        when(builder.build()).thenReturn(client);
                    })) {

                CustomException ex = assertThrows(CustomException.class, () -> {
                    listener.onApplicationEvent(event);
                });

                assertTrue(ex.getMessage().contains("Exception while fetching secret value"));
            }
        }
    }
}

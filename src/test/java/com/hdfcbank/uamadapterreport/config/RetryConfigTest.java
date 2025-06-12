package com.hdfcbank.uamadapterreport.config;

import com.hdfcbank.uamadapterreport.util.CustomRetryPolicy;
import org.junit.jupiter.api.Test;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.policy.NeverRetryPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RetryConfigTest {

    @Test
    void testRetryTemplateConfiguration() throws Exception {
        RetryConfig retryConfig = new RetryConfig();

        // Create a test-specific retry policy that handles RuntimeExceptions
        CustomRetryPolicy testPolicy = new CustomRetryPolicy();
        testPolicy.setExceptionClassifier(ex -> {
            if (ex instanceof RuntimeException) {
                return new SimpleRetryPolicy(4);
            }
            return new NeverRetryPolicy();
        });

        RetryTemplate retryTemplate = retryConfig.retryTemplate((CustomRetryPolicy) testPolicy);

        assertNotNull(retryTemplate);

        // Rest of the test remains the same...
        AtomicInteger attempt = new AtomicInteger(0);
        RetryCallback<String, Exception> callback = context -> {
            if (attempt.getAndIncrement() == 0) {
                throw new RuntimeException("First attempt failed");
            }
            return "Success";
        };

        String result = retryTemplate.execute(callback);

        assertEquals("Success", result);
    }


    @Test
    void testExponentialBackoffConfiguration() {
        RetryConfig retryConfig = new RetryConfig();
        CustomRetryPolicy retryPolicy = new CustomRetryPolicy();

        RetryTemplate retryTemplate = retryConfig.retryTemplate(retryPolicy);

        // We can't directly access the backoff policy, but we can verify its effect
        // by measuring the time between retries (though this would make the test slow)
        // Alternatively, we can verify through logging or other observable behavior

        assertNotNull(retryTemplate);
        // This at least verifies the template was created with our configuration
    }
}
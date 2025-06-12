package com.hdfcbank.uamadapterreport.util;

import com.hdfcbank.uamadapterreport.exception.CustomException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RecoveryCallback;
import org.springframework.retry.support.RetryTemplate;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class RetryAspectTest {

    private RetryTemplate retryTemplate;
    private RetryAspect retryAspect;
    private ProceedingJoinPoint pjp;

    @BeforeEach
    void setUp() {
        retryTemplate = mock(RetryTemplate.class);
        retryAspect = new RetryAspect(retryTemplate);
        pjp = mock(ProceedingJoinPoint.class);
    }

    @Test
    void shouldReturnValueWhenNoException() throws Throwable {
        when(retryTemplate.execute((RetryCallback<Object, RuntimeException>) any(), (RecoveryCallback<Object>) any())).thenAnswer(invocation -> {
            RetryCallback<Object, Throwable> callback = invocation.getArgument(0);
            RetryContext context = mock(RetryContext.class);
            when(context.getRetryCount()).thenReturn(0);
            return callback.doWithRetry(context);
        });

        when(pjp.proceed()).thenReturn("success");

        Object result = retryAspect.aroundRepositoryMethods(pjp);
        assertThat(result).isEqualTo("success");

        verify(pjp).proceed();
    }

    @Test
    void shouldThrowCustomExceptionWhenAllRetriesFail() throws Throwable {
        when(retryTemplate.execute((RetryCallback<Object, RuntimeException>) any(), (RecoveryCallback<Object>) any())).thenAnswer(invocation -> {
            RetryCallback<Object, Throwable> callback = invocation.getArgument(0);
            RecoveryCallback<Object> recoveryCallback = invocation.getArgument(1);

            RetryContext context = mock(RetryContext.class);
            when(context.getRetryCount()).thenReturn(0);

            try {
                return callback.doWithRetry(context);
            } catch (Throwable t) {
                return recoveryCallback.recover(context);
            }
        });

        when(pjp.proceed()).thenThrow(new RuntimeException("fail"));

        assertThatThrownBy(() -> retryAspect.aroundRepositoryMethods(pjp))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("All retries exhausted");

        verify(pjp, atLeastOnce()).proceed();
    }

}

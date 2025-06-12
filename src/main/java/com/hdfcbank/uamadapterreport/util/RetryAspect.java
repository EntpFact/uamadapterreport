    package com.hdfcbank.uamadapterreport.util;

    import com.hdfcbank.uamadapterreport.exception.CustomException;
    import lombok.extern.slf4j.Slf4j;
    import org.aspectj.lang.ProceedingJoinPoint;
    import org.aspectj.lang.annotation.Around;
    import org.aspectj.lang.annotation.Aspect;
    import org.springframework.retry.support.RetryTemplate;
    import org.springframework.stereotype.Component;

    @Aspect
    @Component
    @Slf4j
    public class RetryAspect {

        private final RetryTemplate retryTemplate;

        public RetryAspect(RetryTemplate retryTemplate) {
            this.retryTemplate = retryTemplate;
        }

        @Around("execution(* com.hdfcbank.uamadapterreport.repository..*(..))")

        public Object aroundRepositoryMethods(ProceedingJoinPoint pjp) throws Throwable {
            return retryTemplate.execute(context -> {
                try {
                    return pjp.proceed();
                } catch (Throwable t) {
                    log.error("Retry attempt for method: {}, due to exception: {}", pjp.getSignature(), t.getMessage());
                    // Wrap with your CustomException or keep RuntimeException
                    throw new CustomException("Retry failed at attempt " + context.getRetryCount(), t);
                }
            }, context -> {
                log.warn("All retry attempts failed for method: {}", pjp.getSignature());
                throw new CustomException("All retries exhausted", context.getLastThrowable());
            });
        }

    }

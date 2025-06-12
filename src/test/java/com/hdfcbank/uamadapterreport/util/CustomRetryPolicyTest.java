package com.hdfcbank.uamadapterreport.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.classify.Classifier;
import org.springframework.retry.RetryPolicy;
import org.springframework.retry.policy.NeverRetryPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.test.util.ReflectionTestUtils;

import java.sql.SQLException;
import java.sql.SQLRecoverableException;
import java.sql.SQLTransientConnectionException;
import java.sql.SQLNonTransientConnectionException;
import java.sql.SQLTimeoutException;

import static org.assertj.core.api.Assertions.assertThat;

class CustomRetryPolicyTest {

    private CustomRetryPolicy policy;
    private Classifier<Throwable, RetryPolicy> classifier;

    @BeforeEach
    void setUp() {
        policy = new CustomRetryPolicy();
        policy.init();
        // Get the classifier using the correct interface
        classifier = (Classifier<Throwable, RetryPolicy>)
                ReflectionTestUtils.getField(policy, "exceptionClassifier");
    }

    private RetryPolicy classify(Throwable ex) {
        return classifier.classify(ex);
    }

    @Test
    void retryOnSQLRecoverableException() {
        SQLRecoverableException ex = new SQLRecoverableException();
        RetryPolicy rp = classify(ex);
        assertThat(rp).isInstanceOf(SimpleRetryPolicy.class);
    }

    @Test
    void retryOnSQLTransientConnectionException() {
        SQLTransientConnectionException ex = new SQLTransientConnectionException();
        RetryPolicy rp = classify(ex);
        assertThat(rp).isInstanceOf(SimpleRetryPolicy.class);
    }

    @Test
    void retryOnSQLNonTransientConnectionException() {
        SQLNonTransientConnectionException ex = new SQLNonTransientConnectionException();
        RetryPolicy rp = classify(ex);
        assertThat(rp).isInstanceOf(SimpleRetryPolicy.class);
    }

    @Test
    void retryOnSQLTimeoutException() {
        SQLTimeoutException ex = new SQLTimeoutException();
        RetryPolicy rp = classify(ex);
        assertThat(rp).isInstanceOf(SimpleRetryPolicy.class);
    }

    @Test
    void retryOnSQLExceptionWithMatchingStateCode() {
        SQLException ex = new SQLException("deadlock happened", "40001");
        RetryPolicy rp = classify(ex);
        assertThat(rp).isInstanceOf(SimpleRetryPolicy.class);
    }

    @Test
    void retryOnSQLExceptionWithMappedErrorCodeAndMessage() {
        SQLException ex = new SQLException("Not the leader of this partition", "XX000");
        RetryPolicy rp = classify(ex);
        assertThat(rp).isInstanceOf(SimpleRetryPolicy.class);
    }

    @Test
    void retryOnSQLExceptionWithLegacyMessagePredicate() {
        SQLException ex = new SQLException("Connection is closed by the server");
        RetryPolicy rp = classify(ex);
        assertThat(rp).isInstanceOf(SimpleRetryPolicy.class);
    }

    @Test
    void noRetryOnUnknownSQLException() {
        SQLException ex = new SQLException("some other error", "12345");
        RetryPolicy rp = classify(ex);
        assertThat(rp).isInstanceOf(NeverRetryPolicy.class);
    }

    @Test
    void noRetryOnCompletelyUnrelatedException() {
        Exception ex = new IllegalArgumentException("oops");
        RetryPolicy rp = classify(ex);
        assertThat(rp).isInstanceOf(NeverRetryPolicy.class);
    }

    @Test
    void unwrapsCausesAndRetriesIfInnerMatches() {
        Exception outer = new Exception("wrapper", new SQLRecoverableException());
        RetryPolicy rp = classify(outer);
        assertThat(rp).isInstanceOf(SimpleRetryPolicy.class);
    }

    @Test
    void unwrapsCausesAndNoRetryIfInnerDoesNotMatch() {
        Exception outer = new Exception("wrapper", new SQLException("unknown", "00000"));
        RetryPolicy rp = classify(outer);
        assertThat(rp).isInstanceOf(NeverRetryPolicy.class);
    }
}
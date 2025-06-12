package com.hdfcbank.uamadapterreport.util;

import jakarta.annotation.PostConstruct;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.retry.RetryPolicy;
import org.springframework.retry.policy.ExceptionClassifierRetryPolicy;
import org.springframework.retry.policy.NeverRetryPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionException;

import java.io.Serial;
import java.io.Serializable;
import java.sql.SQLException;
import java.sql.SQLRecoverableException;
import java.sql.SQLTransientConnectionException;
import java.sql.SQLNonTransientConnectionException;
import java.sql.SQLTimeoutException;
import java.util.Map;
import java.util.function.Predicate;
import java.util.regex.Pattern;

@Component
public class CustomRetryPolicy extends ExceptionClassifierRetryPolicy {

    @Serial
    private static final long serialVersionUID = 1L;

    // SQL state codes that should trigger retry
    private static final Pattern SQL_STATE_PATTERN =
            Pattern.compile("^(?:40001|40P01|57P01|08006)$");

    // Map of specific state codes to expected substrings in the message
    private static final Map<String, String> ERROR_CODES =
            Map.of("XX000", "Not the leader");

    // Up to 4 retry attempts
    private final RetryPolicy sp = new SimpleRetryPolicy(4);
    // Never retry
    private final RetryPolicy np = new NeverRetryPolicy();

    /**
     * Matches on SQLState codes or the ERROR_CODES map + message substring.
     */
    private static final Predicate<SQLException> sqlStatePredicate =
            (Predicate<SQLException> & Serializable) ex -> {
                String state = ex.getSQLState();
                if (state != null) {
                    // explicit grouped regex match
                    if (SQL_STATE_PATTERN.matcher(state).matches()) {
                        return true;
                    }
                    // fallback: if we have a mapped substring for this state
                    String expected = ERROR_CODES.get(state);
                    if (expected != null && ex.getMessage() != null) {
                        return ex.getMessage().contains(expected);
                    }
                }
                return false;
            };

    /**
     * “Old-style” contains() checks on the lower-cased message text.
     */
    private static final Predicate<SQLException> sqlMsgPredicate =
            (Predicate<SQLException> & Serializable) ex -> {
                String msg = ex.getMessage();
                if (msg == null) {
                    return false;
                }
                String lower = msg.toLowerCase();
                return lower.contains("connection is closed")
                        || lower.contains("connection reset by peer")
                        || lower.contains("current transaction is expired or aborted");
            };

    /**
     * Other exception types that we consider transient.
     */
    private static final Predicate<Throwable> exceptionPredicate =
            (Predicate<Throwable> & Serializable) ex ->
                    ex instanceof SQLRecoverableException
                            || ex instanceof SQLTransientConnectionException
                            || ex instanceof TransientDataAccessException
                            || ex instanceof TransactionException
                            || ex instanceof SQLNonTransientConnectionException
                            || ex instanceof SQLTimeoutException;

    @PostConstruct
    public void init() {
        this.setExceptionClassifier(cause -> {
            Throwable current = cause;
            while (current != null) {
                // if it's one of the known transient exceptions...
                if (exceptionPredicate.test(current)) {
                    return sp;
                }
                // or if it's an SQLException matching our state/message tests...
                if (current instanceof SQLException sqlEx &&
                        (sqlStatePredicate.or(sqlMsgPredicate).test(sqlEx))) {
                    return sp;
                }
                current = current.getCause();
            }
            // otherwise, never retry
            return np;
        });
    }
}

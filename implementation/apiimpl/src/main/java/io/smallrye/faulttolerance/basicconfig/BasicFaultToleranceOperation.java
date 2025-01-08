package io.smallrye.faulttolerance.basicconfig;

import static io.smallrye.faulttolerance.core.util.Durations.timeInMillis;
import static io.smallrye.faulttolerance.core.util.Preconditions.checkNotNull;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

import org.eclipse.microprofile.faulttolerance.Bulkhead;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.eclipse.microprofile.faulttolerance.exceptions.FaultToleranceDefinitionException;

import io.smallrye.faulttolerance.api.ExponentialBackoff;
import io.smallrye.faulttolerance.api.FibonacciBackoff;
import io.smallrye.faulttolerance.api.RateLimit;
import io.smallrye.faulttolerance.autoconfig.Config;
import io.smallrye.faulttolerance.autoconfig.FaultToleranceMethod;

/**
 * Basic fault tolerance operation metadata. Used for both programmatic and declarative fault tolerance.
 */
public class BasicFaultToleranceOperation {
    protected final String description;

    protected final BulkheadConfig bulkhead;
    protected final CircuitBreakerConfig circuitBreaker;
    protected final RateLimitConfig rateLimit;
    protected final RetryConfig retry;
    protected final TimeoutConfig timeout;

    protected final ExponentialBackoffConfig exponentialBackoff;
    protected final FibonacciBackoffConfig fibonacciBackoff;

    public BasicFaultToleranceOperation(FaultToleranceMethod method) {
        checkNotNull(method, "Method must be set");

        this.description = method.method.toString();

        this.bulkhead = BulkheadConfigImpl.create(method);
        this.circuitBreaker = CircuitBreakerConfigImpl.create(method);
        this.rateLimit = RateLimitConfigImpl.create(method);
        this.retry = RetryConfigImpl.create(method);
        this.timeout = TimeoutConfigImpl.create(method);

        this.exponentialBackoff = ExponentialBackoffConfigImpl.create(method);
        this.fibonacciBackoff = FibonacciBackoffConfigImpl.create(method);
    }

    // `id == null` means no configuration
    public BasicFaultToleranceOperation(String id, Supplier<Bulkhead> bulkhead, Supplier<CircuitBreaker> circuitBreaker,
            Supplier<RateLimit> rateLimit, Supplier<Retry> retry, Supplier<Timeout> timeout,
            Supplier<ExponentialBackoff> exponentialBackoff,
            Supplier<FibonacciBackoff> fibonacciBackoff) {
        this.description = id != null ? id : "<unknown>";
        if (id != null) {
            this.bulkhead = BulkheadConfigImpl.create(id, bulkhead);
            this.circuitBreaker = CircuitBreakerConfigImpl.create(id, circuitBreaker);
            this.rateLimit = RateLimitConfigImpl.create(id, rateLimit);
            this.retry = RetryConfigImpl.create(id, retry);
            this.timeout = TimeoutConfigImpl.create(id, timeout);
            this.exponentialBackoff = ExponentialBackoffConfigImpl.create(id, exponentialBackoff);
            this.fibonacciBackoff = FibonacciBackoffConfigImpl.create(id, fibonacciBackoff);
        } else {
            this.bulkhead = BulkheadNoConfigImpl.create(bulkhead);
            this.circuitBreaker = CircuitBreakerNoConfigImpl.create(circuitBreaker);
            this.rateLimit = RateLimitNoConfigImpl.create(rateLimit);
            this.retry = RetryNoConfigImpl.create(retry);
            this.timeout = TimeoutNoConfigImpl.create(timeout);
            this.exponentialBackoff = ExponentialBackoffNoConfigImpl.create(exponentialBackoff);
            this.fibonacciBackoff = FibonacciBackoffNoConfigImpl.create(fibonacciBackoff);
        }
    }

    public boolean hasBulkhead() {
        return bulkhead != null;
    }

    public Bulkhead getBulkhead() {
        return bulkhead;
    }

    public boolean hasCircuitBreaker() {
        return circuitBreaker != null;
    }

    public CircuitBreaker getCircuitBreaker() {
        return circuitBreaker;
    }

    public boolean hasRateLimit() {
        return rateLimit != null;
    }

    public RateLimit getRateLimit() {
        return rateLimit;
    }

    public boolean hasRetry() {
        return retry != null;
    }

    public Retry getRetry() {
        return retry;
    }

    public boolean hasTimeout() {
        return timeout != null;
    }

    public Timeout getTimeout() {
        return timeout;
    }

    public boolean hasExponentialBackoff() {
        return exponentialBackoff != null;
    }

    public ExponentialBackoff getExponentialBackoff() {
        return exponentialBackoff;
    }

    public boolean hasFibonacciBackoff() {
        return fibonacciBackoff != null;
    }

    public FibonacciBackoff getFibonacciBackoff() {
        return fibonacciBackoff;
    }

    public boolean isValid() {
        try {
            validate();
            return true;
        } catch (FaultToleranceDefinitionException e) {
            return false;
        }
    }

    /**
     * Throws {@link FaultToleranceDefinitionException} if validation fails.
     */
    public void validate() {
        if (bulkhead != null) {
            bulkhead.validate();
        }
        if (circuitBreaker != null) {
            circuitBreaker.validate();
        }
        if (rateLimit != null) {
            rateLimit.validate();
        }
        if (retry != null) {
            retry.validate();
        }
        if (timeout != null) {
            timeout.validate();
        }

        validateRetryBackoff();
    }

    private void validateRetryBackoff() {
        Set<Class<? extends Annotation>> backoffAnnotations = new HashSet<>();

        for (Config cfg : getBackoffConfigs()) {
            if (cfg != null) {
                cfg.validate();
                if (retry == null) {
                    throw cfg.fail("missing @Retry");
                }
                backoffAnnotations.add(cfg.annotationType());
            }
        }

        if (backoffAnnotations.size() > 1) {
            throw new FaultToleranceDefinitionException("More than one backoff defined for " + description
                    + ": " + backoffAnnotations);
        }

        if (retry != null) {
            long retryMaxDuration = timeInMillis(retry.maxDuration(), retry.durationUnit());
            if (retryMaxDuration > 0) {
                if (exponentialBackoff != null) {
                    long maxDelay = timeInMillis(exponentialBackoff.maxDelay(), exponentialBackoff.maxDelayUnit());
                    if (retryMaxDuration <= maxDelay) {
                        throw exponentialBackoff.fail("maxDelay", "should not be greated than @Retry.maxDuration");
                    }
                }

                if (fibonacciBackoff != null) {
                    long maxDelay = timeInMillis(fibonacciBackoff.maxDelay(), fibonacciBackoff.maxDelayUnit());
                    if (retryMaxDuration <= maxDelay) {
                        throw fibonacciBackoff.fail("maxDelay", "should not be greater than @Retry.maxDuration");
                    }
                }
            }
        }
    }

    protected List<Config> getBackoffConfigs() {
        // allows `null` elements, unlike `List.of()`
        return Arrays.asList(exponentialBackoff, fibonacciBackoff);
    }

    /**
     * Ensures all configuration of this fault tolerance operation is loaded. Subsequent method invocations
     * on this instance are guaranteed to not touch MP Config.
     */
    public void materialize() {
        if (bulkhead != null) {
            bulkhead.materialize();
        }
        if (circuitBreaker != null) {
            circuitBreaker.materialize();
        }
        if (rateLimit != null) {
            rateLimit.materialize();
        }
        if (retry != null) {
            retry.materialize();
        }
        if (timeout != null) {
            timeout.materialize();
        }

        if (exponentialBackoff != null) {
            exponentialBackoff.materialize();
        }
        if (fibonacciBackoff != null) {
            fibonacciBackoff.materialize();
        }
    }

    @Override
    public String toString() {
        return "BasicFaultToleranceOperation[" + description + "]";
    }
}

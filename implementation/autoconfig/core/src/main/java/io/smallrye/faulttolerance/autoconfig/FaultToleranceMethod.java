package io.smallrye.faulttolerance.autoconfig;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Set;

import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.Bulkhead;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;

import io.smallrye.common.annotation.Blocking;
import io.smallrye.common.annotation.NonBlocking;
import io.smallrye.faulttolerance.api.ApplyFaultTolerance;
import io.smallrye.faulttolerance.api.AsynchronousNonBlocking;
import io.smallrye.faulttolerance.api.BeforeRetry;
import io.smallrye.faulttolerance.api.CircuitBreakerName;
import io.smallrye.faulttolerance.api.CustomBackoff;
import io.smallrye.faulttolerance.api.ExponentialBackoff;
import io.smallrye.faulttolerance.api.FibonacciBackoff;
import io.smallrye.faulttolerance.api.RateLimit;
import io.smallrye.faulttolerance.api.RetryWhen;

/**
 * Created in the CDI extension to capture effective annotations for each
 * method that needs fault tolerance. By "effective annotation", we mean
 * an annotation that applies to the method, even if it perhaps isn't
 * declared directly on the method. The annotation may be declared on
 * the class that declares the method, or even on a superclass (because
 * Fault Tolerance annotations are generally {@code @Inherited}).
 * <p>
 * Later, {@code FaultToleranceOperation} is created from this class to hold
 * all data the fault tolerance interceptor needs to know about the method.
 * <p>
 * The annotation instances are collected based on what the CDI extension
 * knows, so they don't necessarily correspond to what's in the class
 * bytecode (because CDI extensions may add, remove or modify annotations).
 */
public class FaultToleranceMethod {
    public Class<?> beanClass;
    public MethodDescriptor method;

    public ApplyFaultTolerance applyFaultTolerance;

    public Asynchronous asynchronous;
    public AsynchronousNonBlocking asynchronousNonBlocking;
    public Blocking blocking;
    public NonBlocking nonBlocking;

    public Bulkhead bulkhead;
    public CircuitBreaker circuitBreaker;
    public CircuitBreakerName circuitBreakerName;
    public Fallback fallback;
    public RateLimit rateLimit;
    public Retry retry;
    public Timeout timeout;

    public CustomBackoff customBackoff;
    public ExponentialBackoff exponentialBackoff;
    public FibonacciBackoff fibonacciBackoff;
    public RetryWhen retryWhen;
    public BeforeRetry beforeRetry;

    // types of annotations that were declared directly on the method;
    // other annotations, if present, were declared on the type
    public Set<Class<? extends Annotation>> annotationsPresentDirectly;

    public MethodDescriptor fallbackMethod;
    public List<MethodDescriptor> fallbackMethodsWithExceptionParameter;
    public MethodDescriptor beforeRetryMethod;

    public boolean isLegitimate() {
        if (!KotlinSupport.isLegitimate(method)) {
            return false;
        }

        // certain SmallRye annotations (@CircuitBreakerName, @[Non]Blocking, @*Backoff, @RetryWhen, @BeforeRetry)
        // do _not_ trigger the fault tolerance interceptor alone, only in combination
        // with other fault tolerance annotations
        return applyFaultTolerance != null
                || asynchronous != null
                || asynchronousNonBlocking != null
                || bulkhead != null
                || circuitBreaker != null
                || fallback != null
                || rateLimit != null
                || retry != null
                || timeout != null;
    }
}

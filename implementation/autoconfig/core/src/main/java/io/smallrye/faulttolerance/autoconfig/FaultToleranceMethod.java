package io.smallrye.faulttolerance.autoconfig;

import java.lang.annotation.Annotation;
import java.util.Set;

import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.Bulkhead;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;

import io.smallrye.common.annotation.Blocking;
import io.smallrye.common.annotation.NonBlocking;
import io.smallrye.faulttolerance.api.CircuitBreakerName;
import io.smallrye.faulttolerance.api.CustomBackoff;
import io.smallrye.faulttolerance.api.ExponentialBackoff;
import io.smallrye.faulttolerance.api.FibonacciBackoff;

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

    // MicroProfile Fault Tolerance API
    public Asynchronous asynchronous;
    public Bulkhead bulkhead;
    public CircuitBreaker circuitBreaker;
    public Fallback fallback;
    public Retry retry;
    public Timeout timeout;

    // SmallRye Fault Tolerance API
    public CircuitBreakerName circuitBreakerName;
    public CustomBackoff customBackoff;
    public ExponentialBackoff exponentialBackoff;
    public FibonacciBackoff fibonacciBackoff;

    // SmallRye Common
    public Blocking blocking;
    public NonBlocking nonBlocking;

    // types of annotations that were declared directly on the method;
    // other annotations, if present, were declared on the type
    public Set<Class<? extends Annotation>> annotationsPresentDirectly;

    public boolean isLegitimate() {
        // SmallRye annotations (@CircuitBreakerName, @[Non]Blocking, @*Backoff)
        // alone do _not_ trigger the fault tolerance interceptor,
        // only in combination with other fault tolerance annotations
        return asynchronous != null
                || bulkhead != null
                || circuitBreaker != null
                || fallback != null
                || retry != null
                || timeout != null;
    }
}

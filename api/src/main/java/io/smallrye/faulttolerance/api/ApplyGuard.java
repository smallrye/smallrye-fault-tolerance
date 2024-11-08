package io.smallrye.faulttolerance.api;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.interceptor.InterceptorBinding;

import io.smallrye.common.annotation.Experimental;

/**
 * A special interceptor binding annotation to apply preconfigured fault tolerance.
 * If {@code @ApplyGuard("<identifier>")} is present on a business method,
 * then a bean of type {@link Guard} or {@link TypedGuard} with qualifier
 * {@link io.smallrye.common.annotation.Identifier @Identifier("&lt;identifier>")}
 * must exist. Such bean serves as a preconfigured set of fault tolerance strategies
 * and is used to guard invocations of the annotated business method(s).
 * <p>
 * It is customary to create such bean by declaring a {@code static} producer field.
 * That removes all scoping concerns, because only one instance ever exists. Using
 * a non-static producer field or a producer method means that scoping must be carefully
 * considered, especially if stateful fault tolerance strategies are configured.
 * <p>
 * The {@code @ApplyGuard} annotation may also be present on a bean class,
 * in which case it applies to all business methods declared by the class. If the
 * annotation is present both on the method and the class declaring the method,
 * the one on the method takes precedence.
 * <p>
 * When {@code @ApplyGuard} applies to a business method, all other fault tolerance
 * annotations that would otherwise also apply to that method are ignored.
 * <p>
 * A single preconfigured fault tolerance can be applied to multiple methods.
 * If the preconfigured fault tolerance is of type {@code TypedGuard}, then all methods
 * must have the same return type. If the preconfigured fault tolerance is of type {@code Guard},
 * no such requirement applies; note that in this case, there is no way to define a fallback.
 * <p>
 * Note that this annotation has the same differences to standard MicroProfile Fault Tolerance
 * as {@code Guard} / {@code TypedGuard}:
 * <ul>
 * <li>asynchronous actions of type {@link java.util.concurrent.Future} are not supported;</li>
 * <li>the fallback, circuit breaker and retry strategies always inspect the cause chain of exceptions,
 * following the behavior of SmallRye Fault Tolerance in the non-compatible mode.</li>
 * </ul>
 * If multiple beans of type {@code Guard} or {@code TypedGuard} with the same identifier
 * exist, a deployment problem occurs.
 */
@Inherited
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.TYPE })
@InterceptorBinding
@Experimental("second attempt at providing reusable fault tolerance strategies")
public @interface ApplyGuard {
    /**
     * The identifier of a preconfigured {@link Guard} or {@link TypedGuard} instance.
     */
    String value();
}

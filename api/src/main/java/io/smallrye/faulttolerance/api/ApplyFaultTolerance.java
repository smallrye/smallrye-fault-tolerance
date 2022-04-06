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
 * If {@code @ApplyFaultTolerance("&lt;identifier>")} is present on a business method,
 * then a bean of type {@link FaultTolerance} with qualifier
 * {@link io.smallrye.common.annotation.Identifier @Identifier("&lt;identifier>")}
 * must exist. Such bean serves as a preconfigured set of fault tolerance strategies
 * and is used to guard invocations of the annotated business method(s).
 * <p>
 * It is customary to create such bean by declaring a {@code static} producer field.
 * That removes all scoping concerns, because only one instance ever exists. Using
 * a non-static producer field or a producer method means that scoping must be carefully
 * considered, especially if stateful fault tolerance strategies are configured.
 * <p>
 * The {@code @ApplyFaultTolerance} annotation may also be present on a bean class,
 * in which case it applies to all business methods declared by the class. If the
 * annotation is present both on the method and the class declaring the method,
 * the one on the method takes precedence.
 * <p>
 * When {@code @ApplyFaultTolerance} applies to a business method, all other fault tolerance
 * annotations that would otherwise also apply to that method are ignored.
 * <p>
 * A single preconfigured fault tolerance can be applied to multiple methods, as long as asynchrony
 * of all those methods is the same as the asynchrony of the fault tolerance instance. For example,
 * if the fault tolerance instance is created using {@code FaultTolerance.create()}, it can be
 * applied to all synchronous methods, but not to any asynchronous method. If the fault tolerance
 * instance is created using {@code FaultTolerance.createAsync()}, it can be applied to all
 * asynchronous methods that return {@code CompletionStage}, but not to synchronous methods or
 * asynchronous methods that return any other asynchronous type.
 * <p>
 * A single preconfigured fault tolerance can even be applied to multiple methods with different
 * return types, as long as the constraint on method asynchrony described above is obeyed. In such
 * case, it is customary to declare the fault tolerance instance as {@code FaultTolerance&lt;Object>}
 * for synchronous methods, {@code FaultTolerance&lt;CompletionStage&lt;Object>>} for asynchronous
 * methods that return {@code CompletionStage}, and so on. Note that this effectively precludes
 * defining a useful fallback, because fallback can only be defined when the value type is known.
 */
@Inherited
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.TYPE })
@InterceptorBinding
@Experimental("first attempt at providing reusable fault tolerance strategies")
public @interface ApplyFaultTolerance {
    /**
     * The identifier of a preconfigured fault tolerance instance.
     */
    String value();
}

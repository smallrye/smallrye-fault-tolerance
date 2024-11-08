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
 * @deprecated use {@link ApplyGuard}
 */
@Deprecated(forRemoval = true)
@Inherited
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.TYPE })
@InterceptorBinding
@Experimental("first attempt at providing reusable fault tolerance strategies")
public @interface ApplyFaultTolerance {
    /**
     * @deprecated use {@link ApplyGuard#value()}
     */
    @Deprecated(forRemoval = true)
    String value();
}

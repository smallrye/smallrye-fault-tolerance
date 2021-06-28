package io.smallrye.faulttolerance.autoconfig;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * If an interface that
 * <ul>
 * <li>extends a fault tolerance annotation type,</li>
 * <li>extends the {@link Config} interface,</li>
 * <li>includes a {@code default} implementation of the {@link Config#validate()} method</li>
 * </ul>
 * is annotated {@code @AutoConfig}, an implementation will be generated automatically.
 * The implementation will have a {@code public static} factory method called {@code create}
 * that accepts {@link FaultToleranceMethod}.
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface AutoConfig {
}

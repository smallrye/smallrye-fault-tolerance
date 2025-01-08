package io.smallrye.faulttolerance.autoconfig;

import java.lang.annotation.Annotation;

import org.eclipse.microprofile.faulttolerance.exceptions.FaultToleranceDefinitionException;

public interface Config {
    /**
     * Defines <i>local</i> validation, that is, validation of the single annotation
     * the config interface represents. Global validation (such as cross-checking
     * configuration across multiple annotations) must be done elsewhere.
     * <p>
     * Must be implemented (as a {@code default} method) in the config interface.
     */
    void validate();

    // ---

    /**
     * Returns the type of the annotation that this {@code Config} wraps.
     */
    Class<? extends Annotation> annotationType();

    /**
     * Ensures this configuration is loaded. Subsequent method invocations on this instance
     * are guaranteed to not touch MP Config.
     */
    void materialize();

    /**
     * Returns a new {@link FaultToleranceDefinitionException} for
     * this {@linkplain #annotationType() annotation type} with given {@code reason}.
     */
    FaultToleranceDefinitionException fail(String reason);

    /**
     * Returns a new {@link FaultToleranceDefinitionException} for
     * this {@linkplain #annotationType() annotation type} and its {@code member}
     * with given {@code reason}.
     */
    FaultToleranceDefinitionException fail(String member, String reason);
}

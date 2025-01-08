package io.smallrye.faulttolerance.autoconfig;

import java.lang.annotation.Annotation;

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

    Class<?> beanClass();

    MethodDescriptor method();

    // defined by `Annotation`, so for convenience, we expose it here too
    Class<? extends Annotation> annotationType();

    /**
     * Returns whether the annotation is present on method or not
     * (in which case, it is present on the class). This is useful
     * when two annotations conflict, in which case the one on method
     * has priority over the one on class.
     */
    boolean isOnMethod();

    /**
     * Ensures this configuration is loaded. Subsequent method invocations on this instance
     * are guaranteed to not touch MP Config.
     */
    void materialize();
}

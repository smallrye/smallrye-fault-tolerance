package io.smallrye.faulttolerance.autoconfig;

/**
 * Specialization of {@link Config} that should be used when the given annotation
 * can only be configured in the declarative, annotation-based API. A config
 * interface for an annotation that can also be configured in the programmatic API
 * should implement just {@code Config}.
 */
public interface ConfigDeclarativeOnly extends Config {
    /**
     * Returns whether the annotation is present on method or not
     * (in which case, it is present on the class). This is useful
     * when two annotations conflict, in which case the one on method
     * has priority over the one on class.
     */
    boolean isOnMethod();

    Class<?> beanClass();

    MethodDescriptor method();
}

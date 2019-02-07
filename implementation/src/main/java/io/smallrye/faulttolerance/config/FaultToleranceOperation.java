/*
 * Copyright 2017 Red Hat, Inc, and individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.smallrye.faulttolerance.config;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Future;
import java.util.function.BiFunction;
import java.util.function.Function;

import javax.enterprise.inject.spi.AnnotatedMethod;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.Bulkhead;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.eclipse.microprofile.faulttolerance.exceptions.FaultToleranceDefinitionException;

/**
 * Fault tolerance operation metadata.
 *
 * @author Martin Kouba
 */
public class FaultToleranceOperation {

    public static FaultToleranceOperation of(AnnotatedMethod<?> annotatedMethod) {
        return new FaultToleranceOperation(annotatedMethod.getDeclaringType().getJavaClass(), annotatedMethod.getJavaMember(),
                                           isAsync(annotatedMethod),
                                           returnsCompletionStage(annotatedMethod),
                                           getConfig(Bulkhead.class, annotatedMethod, BulkheadConfig::new),
                                           getConfig(CircuitBreaker.class, annotatedMethod, CircuitBreakerConfig::new),
                                           getConfig(Fallback.class, annotatedMethod, FallbackConfig::new),
                                           getConfig(Retry.class, annotatedMethod, RetryConfig::new),
                                           getConfig(Timeout.class, annotatedMethod, TimeoutConfig::new));
    }

    public static FaultToleranceOperation of(Class<?> beanClass, Method method) {
        return new FaultToleranceOperation(beanClass,method,
                                           isAsync(method, beanClass),
                                           returnsCompletionStage(method),
                                           getConfig(Bulkhead.class, beanClass,method, BulkheadConfig::new),
                                           getConfig(CircuitBreaker.class, beanClass,method, CircuitBreakerConfig::new),
                                           getConfig(Fallback.class, beanClass,method, FallbackConfig::new),
                                           getConfig(Retry.class, beanClass,method, RetryConfig::new),
                                           getConfig(Timeout.class, beanClass, method, TimeoutConfig::new));
    }

    private final Class<?> beanClass;

    private final Method method;

    private final boolean async;

    private final boolean returnsCompletionStage;

    private final BulkheadConfig bulkhead;

    private final CircuitBreakerConfig circuitBreaker;

    private final FallbackConfig fallback;

    private final RetryConfig retry;

    private final TimeoutConfig timeout;

    private FaultToleranceOperation(Class<?> beanClass,
                                    Method method,
                                    boolean async,
                                    boolean returnsCompletionStage,
                                    BulkheadConfig bulkhead,
                                    CircuitBreakerConfig circuitBreaker,
                                    FallbackConfig fallback,
                                    RetryConfig retry,
                                    TimeoutConfig timeout) {
        this.beanClass = beanClass;
        this.method = method;
        this.async = async;
        this.returnsCompletionStage = returnsCompletionStage;
        this.bulkhead = bulkhead;
        this.circuitBreaker = circuitBreaker;
        this.fallback = fallback;
        this.retry = retry;
        this.timeout = timeout;
    }

    public boolean isAsync() {
        return async;
    }

    public boolean returnsCompletionStage() {
        return returnsCompletionStage;
    }

    public boolean hasBulkhead() {
        return bulkhead != null;
    }

    public BulkheadConfig getBulkhead() {
        return bulkhead;
    }

    public CircuitBreakerConfig getCircuitBreaker() {
        return circuitBreaker;
    }

    public boolean hasCircuitBreaker() {
        return circuitBreaker != null;
    }

    public FallbackConfig getFallback() {
        return fallback;
    }

    public boolean hasFallback() {
        return fallback != null;
    }

    public RetryConfig getRetry() {
        return retry;
    }

    public boolean hasRetry() {
        return retry != null;
    }

    public TimeoutConfig getTimeout() {
        return timeout;
    }

    public boolean hasTimeout() {
        return timeout != null;
    }

    public Method getMethod() {
        return method;
    }

    public Class<?> getBeanClass() {
        return beanClass;
    }

    public boolean isLegitimate() {
        return async || bulkhead != null || circuitBreaker != null || fallback != null || retry != null || timeout != null;
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
        if (async && !isAcceptableAsyncReturnType(method.getReturnType())) {
            throw new FaultToleranceDefinitionException("Invalid @Asynchronous on " + method + " : the return type must be java.util.concurrent.Future");
        }
        if (bulkhead != null) {
            bulkhead.validate();
        }
        if (circuitBreaker != null) {
            circuitBreaker.validate();
        }
        if (fallback != null) {
            fallback.validate();
        }
        if (retry != null) {
            retry.validate();
        }
        if (timeout != null) {
            timeout.validate();
        }
    }

    private boolean isAcceptableAsyncReturnType(Class<?> returnType) {
        return Future.class.equals(returnType) || CompletionStage.class.equals(returnType);
    }

    @Override
    public String toString() {
        return "FaultToleranceOperation [beanClass=" + beanClass + ", method=" + method.toGenericString() + "]";
    }


    private static <A extends Annotation, C extends GenericConfig<A>> C getConfig(Class<A> annotationType, AnnotatedMethod<?> annotatedMethod,
                                                                                  Function<AnnotatedMethod<?>, C> function) {
        if (getConfigStatus(annotationType, annotatedMethod.getJavaMember()) && isAnnotated(annotationType, annotatedMethod)) {
            return function.apply(annotatedMethod);
        }
        return null;
    }


    private static boolean returnsCompletionStage(Method annotatedMethod) {
        return CompletionStage.class.isAssignableFrom(annotatedMethod.getReturnType());
    }
    
    private static boolean returnsCompletionStage(AnnotatedMethod<?> annotatedMethod) {
        return returnsCompletionStage(annotatedMethod.getJavaMember());
    }

    private static boolean isAsync(Method method, Class<?> beanClass) {

        return getConfigStatus(Asynchronous.class, method) && isAnnotated(Asynchronous.class, method, beanClass);
    }

    private static boolean isAsync(AnnotatedMethod<?> method) {

        return getConfigStatus(Asynchronous.class, method.getJavaMember()) && isAnnotated(Asynchronous.class, method);
    }

    private static <A extends Annotation> boolean isAnnotated(Class<A> annotationType, AnnotatedMethod<?> annotatedMethod) {
        return annotatedMethod.isAnnotationPresent(annotationType) || annotatedMethod.getDeclaringType().isAnnotationPresent(annotationType);
    }

    private static <A extends Annotation, C extends GenericConfig<A>> C getConfig(Class<A> annotationType, Class<?> beanClass, Method method,
            BiFunction<Class<?>, Method, C> function) {

        if (getConfigStatus(annotationType, method) && isAnnotated(annotationType, method, beanClass)) {
            return function.apply(beanClass, method);
        }
        return null;
    }

    private static <A extends Annotation> Boolean getConfigStatus(Class<A> annotationType, Method method) {
        Config config = ConfigProvider.getConfig();
        final String undifined = "undifined";
        String onMethod = config.getOptionalValue(method.getDeclaringClass().getName() +
                          "/" + method.getName() + "/" + annotationType.getSimpleName() + "/enabled", String.class).orElse(undifined);
        String onClass = config.getOptionalValue(method.getDeclaringClass().getName() +
                          "/" + annotationType.getSimpleName() + "/enabled", String.class).orElse(undifined);
        String onGlobal = config.getOptionalValue(annotationType.getSimpleName() + "/enabled", String.class).orElse(undifined);
        Boolean returnConfig = true;

        if (!undifined.equals(onMethod)) {
            returnConfig = new Boolean(onMethod);
        } else if (!undifined.equals(onClass)) {
            returnConfig = new Boolean(onClass);
        } else if (!undifined.equals(onGlobal)) {
            returnConfig = new Boolean(onGlobal);
        }
        return returnConfig;
    }

    private static <A extends Annotation> boolean isAnnotated(Class<A> annotationType, Method method, Class<?> beanClass) {
        if (method.isAnnotationPresent(annotationType)) {
            return true;
        }
        if (beanClass.isAnnotationPresent(annotationType)) {
            return true;
        }
        while (beanClass != null) {
            if (beanClass.isAnnotationPresent(annotationType)) {
                return true;
            }
            beanClass = beanClass.getSuperclass();
        }
        return false;
    }

}

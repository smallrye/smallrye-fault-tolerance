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
import java.util.StringJoiner;
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

import io.smallrye.common.annotation.Blocking;
import io.smallrye.common.annotation.NonBlocking;
import io.smallrye.faulttolerance.AsyncTypes;
import io.smallrye.reactive.converters.ReactiveTypeConverter;

/**
 * Fault tolerance operation metadata.
 *
 * @author Martin Kouba
 */
public class FaultToleranceOperation {

    public static FaultToleranceOperation of(AnnotatedMethod<?> annotatedMethod) {
        return new FaultToleranceOperation(annotatedMethod.getDeclaringType().getJavaClass(), annotatedMethod.getJavaMember(),
                isAsync(annotatedMethod),
                isBlocking(annotatedMethod) || isNonBlocking(annotatedMethod),
                isThreadOffloadRequired(annotatedMethod),
                returnType(annotatedMethod),
                getConfig(Bulkhead.class, annotatedMethod, BulkheadConfig::new),
                getConfig(CircuitBreaker.class, annotatedMethod, CircuitBreakerConfig::new),
                getConfig(Fallback.class, annotatedMethod, FallbackConfig::new),
                getConfig(Retry.class, annotatedMethod, RetryConfig::new),
                getConfig(Timeout.class, annotatedMethod, TimeoutConfig::new));
    }

    public static FaultToleranceOperation of(Class<?> beanClass, Method method) {
        return new FaultToleranceOperation(beanClass, method,
                isAsync(method, beanClass),
                isBlocking(method, beanClass) || isNonBlocking(method, beanClass),
                isThreadOffloadRequired(method, beanClass),
                returnType(method),
                getConfig(Bulkhead.class, beanClass, method, BulkheadConfig::new),
                getConfig(CircuitBreaker.class, beanClass, method, CircuitBreakerConfig::new),
                getConfig(Fallback.class, beanClass, method, FallbackConfig::new),
                getConfig(Retry.class, beanClass, method, RetryConfig::new),
                getConfig(Timeout.class, beanClass, method, TimeoutConfig::new));
    }

    private final Class<?> beanClass;

    private final Method method;

    // whether @Asynchronous is present
    private final boolean async;

    // whether @Blocking or @NonBlocking is present
    private final boolean additionalAsync;

    // whether thread offload is required based on presence or absence of @Blocking and @NonBlocking
    // if the guarded method doesn't return CompletionStage, this value is meaningless
    private final boolean threadOffloadRequired;

    private final Class<?> returnType;

    private final BulkheadConfig bulkhead;

    private final CircuitBreakerConfig circuitBreaker;

    private final FallbackConfig fallback;

    private final RetryConfig retry;

    private final TimeoutConfig timeout;

    private FaultToleranceOperation(Class<?> beanClass,
            Method method,
            boolean async,
            boolean additionalAsync,
            boolean threadOffloadRequired,
            Class<?> returnType,
            BulkheadConfig bulkhead,
            CircuitBreakerConfig circuitBreaker,
            FallbackConfig fallback,
            RetryConfig retry,
            TimeoutConfig timeout) {
        this.beanClass = beanClass;
        this.method = method;
        this.async = async;
        this.additionalAsync = additionalAsync;
        this.threadOffloadRequired = threadOffloadRequired;
        this.returnType = returnType;
        this.bulkhead = bulkhead;
        this.circuitBreaker = circuitBreaker;
        this.fallback = fallback;
        this.retry = retry;
        this.timeout = timeout;
    }

    public boolean isAsync() {
        return async;
    }

    public boolean isAdditionalAsync() {
        return additionalAsync;
    }

    public boolean isThreadOffloadRequired() {
        return threadOffloadRequired;
    }

    public Class<?> getReturnType() {
        return returnType;
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
        // @Blocking and @NonBlocking alone do _not_ trigger the fault tolerance interceptor,
        // only in combination with other fault tolerance annotations
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
            throw new FaultToleranceDefinitionException("Invalid @Asynchronous on " + method
                    + ": must return java.util.concurrent.Future or " + describeAsyncReturnTypes());
        }
        if (additionalAsync && !AsyncTypes.isKnown(method.getReturnType())) {
            throw new FaultToleranceDefinitionException("Invalid @Blocking/@NonBlocking on " + method
                    + ": must return " + describeAsyncReturnTypes());
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
        return Future.class.equals(returnType) || AsyncTypes.isKnown(returnType);
    }

    @Override
    public String toString() {
        return "FaultToleranceOperation [beanClass=" + beanClass + ", method=" + method.toGenericString() + "]";
    }

    private static String describeAsyncReturnTypes() {
        StringJoiner result = new StringJoiner(" or ");
        for (ReactiveTypeConverter<?> converter : AsyncTypes.allKnown()) {
            result.add(converter.type().getName());
        }
        return result.toString();
    }

    private static Class<?> returnType(Method annotatedMethod) {
        return annotatedMethod.getReturnType();
    }

    private static Class<?> returnType(AnnotatedMethod<?> annotatedMethod) {
        return annotatedMethod.getJavaMember().getReturnType();
    }

    private static boolean isAsync(Method method, Class<?> beanClass) {
        return getConfigStatus(Asynchronous.class, method) && isAnnotated(Asynchronous.class, method, beanClass);
    }

    private static boolean isAsync(AnnotatedMethod<?> method) {
        return getConfigStatus(Asynchronous.class, method.getJavaMember()) && isAnnotated(Asynchronous.class, method);
    }

    private static boolean isBlocking(Method method, Class<?> beanClass) {
        return getConfigStatus(Blocking.class, method) && isAnnotated(Blocking.class, method, beanClass);
    }

    private static boolean isBlocking(AnnotatedMethod<?> method) {
        return getConfigStatus(Blocking.class, method.getJavaMember()) && isAnnotated(Blocking.class, method);
    }

    private static boolean isNonBlocking(Method method, Class<?> beanClass) {
        return getConfigStatus(NonBlocking.class, method) && isAnnotated(NonBlocking.class, method, beanClass);
    }

    private static boolean isNonBlocking(AnnotatedMethod<?> method) {
        return getConfigStatus(NonBlocking.class, method.getJavaMember()) && isAnnotated(NonBlocking.class, method);
    }

    private static <A extends Annotation, C extends GenericConfig<A>> C getConfig(Class<A> annotationType,
            AnnotatedMethod<?> annotatedMethod, Function<AnnotatedMethod<?>, C> function) {
        if (getConfigStatus(annotationType, annotatedMethod.getJavaMember()) && isAnnotated(annotationType, annotatedMethod)) {
            return function.apply(annotatedMethod);
        }
        return null;
    }

    private static <A extends Annotation, C extends GenericConfig<A>> C getConfig(Class<A> annotationType, Class<?> beanClass,
            Method method, BiFunction<Class<?>, Method, C> function) {

        if (getConfigStatus(annotationType, method) && isAnnotated(annotationType, method, beanClass)) {
            return function.apply(beanClass, method);
        }
        return null;
    }

    private static <A extends Annotation> boolean getConfigStatus(Class<A> annotationType, Method method) {
        Config config = ConfigProvider.getConfig();
        final String undefined = "undefined";
        String onMethod = config.getOptionalValue(method.getDeclaringClass().getName() +
                "/" + method.getName() + "/" + annotationType.getSimpleName() + "/enabled", String.class).orElse(undefined);
        String onClass = config.getOptionalValue(method.getDeclaringClass().getName() +
                "/" + annotationType.getSimpleName() + "/enabled", String.class).orElse(undefined);
        String onGlobal = config.getOptionalValue(annotationType.getSimpleName() + "/enabled", String.class).orElse(undefined);
        boolean returnConfig = !annotationType.equals(Fallback.class)
                ? config.getOptionalValue("MP_Fault_Tolerance_NonFallback_Enabled", Boolean.class).orElse(true)
                : true;

        if (!undefined.equals(onMethod)) {
            returnConfig = Boolean.parseBoolean(onMethod);
        } else if (!undefined.equals(onClass)) {
            returnConfig = Boolean.parseBoolean(onClass);
        } else if (!undefined.equals(onGlobal)) {
            returnConfig = Boolean.parseBoolean(onGlobal);
        }
        return returnConfig;
    }

    private static <A extends Annotation> boolean isAnnotated(Class<A> annotationType, AnnotatedMethod<?> annotatedMethod) {
        return annotatedMethod.isAnnotationPresent(annotationType)
                || annotatedMethod.getDeclaringType().isAnnotationPresent(annotationType);
    }

    private static <A extends Annotation> boolean isAnnotated(Class<A> annotationType, Method method, Class<?> beanClass) {
        if (method.isAnnotationPresent(annotationType)) {
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

    // @Blocking and @NonBlocking can currently only be used on methods, but that will likely change,
    // so we'd better define meaning for class annotations as well
    // the result of isThreadOffloadRequired only makes sense if the method in question returns CompletionStage

    private static boolean isThreadOffloadRequired(AnnotatedMethod<?> method) {
        if (method.isAnnotationPresent(Blocking.class)) {
            return true;
        }
        if (method.isAnnotationPresent(NonBlocking.class)) {
            return false;
        }

        if (method.getDeclaringType().isAnnotationPresent(Blocking.class)) {
            return true;
        }
        if (method.getDeclaringType().isAnnotationPresent(NonBlocking.class)) {
            return false;
        }

        // no @Blocking or @NonBlocking, we should offload to another thread as that's MP FT default
        return true;
    }

    private static boolean isThreadOffloadRequired(Method method, Class<?> beanClass) {
        if (method.isAnnotationPresent(Blocking.class)) {
            return true;
        }
        if (method.isAnnotationPresent(NonBlocking.class)) {
            return false;
        }

        while (beanClass != null) {
            if (beanClass.isAnnotationPresent(Blocking.class)) {
                return true;
            }
            if (beanClass.isAnnotationPresent(NonBlocking.class)) {
                return false;
            }
            beanClass = beanClass.getSuperclass();
        }

        // no @Blocking or @NonBlocking, we should offload to another thread as that's MP FT default
        return true;
    }
}

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
package io.smallrye.faulttolerance;

import static io.smallrye.faulttolerance.CdiLogger.LOG;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import jakarta.annotation.Priority;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.AfterDeploymentValidation;
import jakarta.enterprise.inject.spi.AnnotatedConstructor;
import jakarta.enterprise.inject.spi.AnnotatedField;
import jakarta.enterprise.inject.spi.AnnotatedMethod;
import jakarta.enterprise.inject.spi.AnnotatedType;
import jakarta.enterprise.inject.spi.Bean;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.BeforeBeanDiscovery;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.inject.spi.ProcessAnnotatedType;
import jakarta.enterprise.inject.spi.ProcessBean;
import jakarta.enterprise.inject.spi.ProcessManagedBean;
import jakarta.enterprise.util.AnnotationLiteral;

import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.Bulkhead;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;

import io.smallrye.common.annotation.Blocking;
import io.smallrye.common.annotation.Identifier;
import io.smallrye.common.annotation.NonBlocking;
import io.smallrye.faulttolerance.api.ApplyFaultTolerance;
import io.smallrye.faulttolerance.api.ApplyGuard;
import io.smallrye.faulttolerance.api.AsynchronousNonBlocking;
import io.smallrye.faulttolerance.api.BeforeRetry;
import io.smallrye.faulttolerance.api.CustomBackoff;
import io.smallrye.faulttolerance.api.ExponentialBackoff;
import io.smallrye.faulttolerance.api.FibonacciBackoff;
import io.smallrye.faulttolerance.api.Guard;
import io.smallrye.faulttolerance.api.RateLimit;
import io.smallrye.faulttolerance.api.RetryWhen;
import io.smallrye.faulttolerance.api.TypedGuard;
import io.smallrye.faulttolerance.autoconfig.FaultToleranceMethod;
import io.smallrye.faulttolerance.config.FaultToleranceMethods;
import io.smallrye.faulttolerance.config.FaultToleranceOperation;
import io.smallrye.faulttolerance.internal.StrategyCache;
import io.smallrye.faulttolerance.metrics.CompoundMetricsProvider;
import io.smallrye.faulttolerance.metrics.MetricsIntegration;
import io.smallrye.faulttolerance.metrics.MicroProfileMetricsProvider;
import io.smallrye.faulttolerance.metrics.MicrometerProvider;
import io.smallrye.faulttolerance.metrics.NoopProvider;
import io.smallrye.faulttolerance.metrics.OpenTelemetryProvider;

public class FaultToleranceExtension implements Extension {

    private static final List<Class<? extends Annotation>> BACKOFF_ANNOTATIONS = Arrays.asList(
            ExponentialBackoff.class,
            FibonacciBackoff.class,
            CustomBackoff.class);

    /**
     * @see #collectFaultToleranceOperations(ProcessManagedBean)
     */
    private final ConcurrentMap<String, FaultToleranceOperation> faultToleranceOperations = new ConcurrentHashMap<>();

    private final ConcurrentMap<String, Set<String>> existingCircuitBreakerNames = new ConcurrentHashMap<>();

    private final ConcurrentMap<String, Set<String>> existingGuards = new ConcurrentHashMap<>();

    private final Set<String> expectedGuards = ConcurrentHashMap.newKeySet();

    private final Set<MetricsIntegration> metricsIntegrations;

    private static boolean isPresent(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException ignored) {
            // not present
            return false;
        }
    }

    private static Set<MetricsIntegration> allPresentMetrics() {
        Set<MetricsIntegration> result = EnumSet.noneOf(MetricsIntegration.class);
        if (isPresent("org.eclipse.microprofile.metrics.MetricRegistry")) {
            result.add(MetricsIntegration.MICROPROFILE_METRICS);
        }
        if (isPresent("io.opentelemetry.api.metrics.Meter")) {
            result.add(MetricsIntegration.OPENTELEMETRY);
        }
        if (isPresent("io.micrometer.core.instrument.MeterRegistry")) {
            result.add(MetricsIntegration.MICROMETER);
        }
        if (result.isEmpty()) {
            result.add(MetricsIntegration.NOOP);
        }
        return result;
    }

    public FaultToleranceExtension() {
        this(allPresentMetrics());
    }

    public FaultToleranceExtension(MetricsIntegration metricsIntegration) {
        this(EnumSet.of(metricsIntegration));
    }

    public FaultToleranceExtension(Set<MetricsIntegration> metricsIntegrations) {
        this.metricsIntegrations = EnumSet.copyOf(metricsIntegrations);
    }

    void registerInterceptorBindings(@Observes BeforeBeanDiscovery bbd, BeanManager bm) {
        LOG.activated(getImplementationVersion().orElse("unknown"));

        // certain SmallRye annotations (@CircuitBreakerName, @[Non]Blocking, @*Backoff, @RetryWhen, @BeforeRetry)
        // do _not_ trigger the fault tolerance interceptor alone, only in combination
        // with other fault tolerance annotations
        bbd.addInterceptorBinding(new FTInterceptorBindingAnnotatedType<>(bm.createAnnotatedType(ApplyFaultTolerance.class)));
        bbd.addInterceptorBinding(new FTInterceptorBindingAnnotatedType<>(bm.createAnnotatedType(ApplyGuard.class)));
        bbd.addInterceptorBinding(new FTInterceptorBindingAnnotatedType<>(bm.createAnnotatedType(Asynchronous.class)));
        bbd.addInterceptorBinding(new FTInterceptorBindingAnnotatedType<>(
                bm.createAnnotatedType(AsynchronousNonBlocking.class)));
        bbd.addInterceptorBinding(new FTInterceptorBindingAnnotatedType<>(bm.createAnnotatedType(Bulkhead.class)));
        bbd.addInterceptorBinding(new FTInterceptorBindingAnnotatedType<>(bm.createAnnotatedType(CircuitBreaker.class)));
        bbd.addInterceptorBinding(new FTInterceptorBindingAnnotatedType<>(bm.createAnnotatedType(Fallback.class)));
        bbd.addInterceptorBinding(new FTInterceptorBindingAnnotatedType<>(bm.createAnnotatedType(RateLimit.class)));
        bbd.addInterceptorBinding(new FTInterceptorBindingAnnotatedType<>(bm.createAnnotatedType(Retry.class)));
        bbd.addInterceptorBinding(new FTInterceptorBindingAnnotatedType<>(bm.createAnnotatedType(Timeout.class)));

        bbd.addAnnotatedType(bm.createAnnotatedType(FaultToleranceInterceptor.class),
                FaultToleranceInterceptor.class.getName());
        bbd.addAnnotatedType(bm.createAnnotatedType(DefaultFallbackHandlerProvider.class),
                DefaultFallbackHandlerProvider.class.getName());
        bbd.addAnnotatedType(bm.createAnnotatedType(DefaultBeforeRetryHandlerProvider.class),
                DefaultBeforeRetryHandlerProvider.class.getName());
        bbd.addAnnotatedType(bm.createAnnotatedType(DefaultAsyncExecutorProvider.class),
                DefaultAsyncExecutorProvider.class.getName());
        bbd.addAnnotatedType(bm.createAnnotatedType(ExecutorHolder.class),
                ExecutorHolder.class.getName());
        bbd.addAnnotatedType(bm.createAnnotatedType(DefaultFaultToleranceOperationProvider.class),
                DefaultFaultToleranceOperationProvider.class.getName());
        bbd.addAnnotatedType(bm.createAnnotatedType(DefaultExistingCircuitBreakerNames.class),
                DefaultExistingCircuitBreakerNames.class.getName());
        bbd.addAnnotatedType(bm.createAnnotatedType(StrategyCache.class), StrategyCache.class.getName());
        bbd.addAnnotatedType(bm.createAnnotatedType(CircuitBreakerMaintenanceImpl.class),
                CircuitBreakerMaintenanceImpl.class.getName());
        bbd.addAnnotatedType(bm.createAnnotatedType(RequestContextIntegration.class),
                RequestContextIntegration.class.getName());
        bbd.addAnnotatedType(bm.createAnnotatedType(SpecCompatibility.class), SpecCompatibility.class.getName());
        bbd.addAnnotatedType(bm.createAnnotatedType(Enablement.class), Enablement.class.getName());
        bbd.addAnnotatedType(bm.createAnnotatedType(CdiSpi.EagerDependencies.class),
                CdiSpi.EagerDependencies.class.getName());
        bbd.addAnnotatedType(bm.createAnnotatedType(CdiSpi.LazyDependencies.class),
                CdiSpi.LazyDependencies.class.getName());

        if (metricsIntegrations.size() > 1) {
            bbd.addAnnotatedType(bm.createAnnotatedType(CompoundMetricsProvider.class),
                    CompoundMetricsProvider.class.getName());
        }
        for (MetricsIntegration metricsIntegration : metricsIntegrations) {
            switch (metricsIntegration) {
                case MICROPROFILE_METRICS:
                    bbd.addAnnotatedType(bm.createAnnotatedType(MicroProfileMetricsProvider.class),
                            MicroProfileMetricsProvider.class.getName());
                    break;
                case OPENTELEMETRY:
                    bbd.addAnnotatedType(bm.createAnnotatedType(OpenTelemetryProvider.class),
                            OpenTelemetryProvider.class.getName());
                    break;
                case MICROMETER:
                    bbd.addAnnotatedType(bm.createAnnotatedType(MicrometerProvider.class),
                            MicrometerProvider.class.getName());
                    break;
                case NOOP:
                    bbd.addAnnotatedType(bm.createAnnotatedType(NoopProvider.class),
                            NoopProvider.class.getName());
                    break;
            }
        }
    }

    void changeInterceptorPriority(@Observes ProcessAnnotatedType<FaultToleranceInterceptor> event) {
        ConfigProvider.getConfig()
                .getOptionalValue("mp.fault.tolerance.interceptor.priority", Integer.class)
                .ifPresent(configuredInterceptorPriority -> {
                    event.configureAnnotatedType()
                            .remove(ann -> ann instanceof Priority)
                            .add(new PriorityLiteral(configuredInterceptorPriority));
                });
    }

    /**
     * Observe all enabled managed beans and identify/validate FT operations. This allows us to:
     * <ul>
     * <li>Skip validation of types which are not recognized as beans (e.g. are vetoed)</li>
     * <li>Take the final values of AnnotatedTypes</li>
     * <li>Support annotations added via portable extensions</li>
     * </ul>
     *
     * @param event
     */
    void collectFaultToleranceOperations(@Observes ProcessManagedBean<?> event) {
        AnnotatedType<?> annotatedType = event.getAnnotatedBeanClass();
        for (AnnotatedMethod<?> annotatedMethod : annotatedType.getMethods()) {
            if (annotatedMethod.getJavaMember().isSynthetic()) {
                continue;
            }

            FaultToleranceMethod method = FaultToleranceMethods.create(annotatedType.getJavaClass(), annotatedMethod);
            if (method.isLegitimate()) {
                FaultToleranceOperation operation = new FaultToleranceOperation(method);
                operation.validate();
                LOG.debugf("Found %s", operation);
                faultToleranceOperations.put(getCacheKey(annotatedType.getJavaClass(), annotatedMethod.getJavaMember()),
                        operation);

                if (operation.hasCircuitBreaker() && operation.hasCircuitBreakerName()) {
                    existingCircuitBreakerNames
                            .computeIfAbsent(operation.getCircuitBreakerName().value(), ignored -> new HashSet<>())
                            .add(annotatedMethod.getJavaMember().toGenericString());
                }

                if (operation.hasApplyGuard()) {
                    expectedGuards.add(operation.getApplyGuard().value());
                }

                for (Class<? extends Annotation> backoffAnnotation : BACKOFF_ANNOTATIONS) {
                    if (annotatedMethod.isAnnotationPresent(backoffAnnotation)
                            && !annotatedMethod.isAnnotationPresent(Retry.class)) {
                        event.addDefinitionError(LOG.backoffAnnotationWithoutRetry(backoffAnnotation.getSimpleName(),
                                method.method));
                    }
                    if (annotatedType.isAnnotationPresent(backoffAnnotation)
                            && !annotatedType.isAnnotationPresent(Retry.class)) {
                        event.addDefinitionError(LOG.backoffAnnotationWithoutRetry(backoffAnnotation.getSimpleName(),
                                annotatedType.getJavaClass()));
                    }
                }

                if (annotatedMethod.isAnnotationPresent(RetryWhen.class)
                        && !annotatedMethod.isAnnotationPresent(Retry.class)) {
                    event.addDefinitionError(LOG.retryWhenAnnotationWithoutRetry(method.method));
                }

                if (annotatedType.isAnnotationPresent(RetryWhen.class)
                        && !annotatedType.isAnnotationPresent(Retry.class)) {
                    event.addDefinitionError(LOG.retryWhenAnnotationWithoutRetry(annotatedType.getJavaClass()));
                }

                if (annotatedMethod.isAnnotationPresent(BeforeRetry.class)
                        && !annotatedMethod.isAnnotationPresent(Retry.class)) {
                    event.addDefinitionError(LOG.beforeRetryAnnotationWithoutRetry(method.method));
                }

                if (annotatedType.isAnnotationPresent(BeforeRetry.class)
                        && !annotatedType.isAnnotationPresent(Retry.class)) {
                    event.addDefinitionError(LOG.beforeRetryAnnotationWithoutRetry(annotatedType.getJavaClass()));
                }

                if (annotatedMethod.isAnnotationPresent(Asynchronous.class)
                        && annotatedMethod.isAnnotationPresent(AsynchronousNonBlocking.class)) {
                    event.addDefinitionError(LOG.bothAsyncAndAsyncNonBlockingPresent(method.method));
                }

                if (annotatedType.isAnnotationPresent(Asynchronous.class)
                        && annotatedType.isAnnotationPresent(AsynchronousNonBlocking.class)) {
                    event.addDefinitionError(LOG.bothAsyncAndAsyncNonBlockingPresent(annotatedType.getJavaClass()));
                }

                if (annotatedMethod.isAnnotationPresent(Blocking.class)
                        && annotatedMethod.isAnnotationPresent(NonBlocking.class)) {
                    event.addDefinitionError(LOG.bothBlockingNonBlockingPresent(method.method));
                }

                if (annotatedType.isAnnotationPresent(Blocking.class)
                        && annotatedType.isAnnotationPresent(NonBlocking.class)) {
                    event.addDefinitionError(LOG.bothBlockingNonBlockingPresent(annotatedType.getJavaClass()));
                }
            }
        }
    }

    void processBean(@Observes ProcessBean<?> pb) {
        Bean<?> bean = pb.getBean();
        boolean isGuard = bean.getTypes().contains(Guard.class)
                || bean.getTypes().contains(TypedGuard.class)
                || bean.getTypes().stream().anyMatch(it -> it instanceof ParameterizedType
                        && ((ParameterizedType) it).getRawType().equals(TypedGuard.class));
        if (isGuard) {
            for (Annotation ann : bean.getQualifiers()) {
                if (ann instanceof Identifier) {
                    String id = ((Identifier) ann).value();
                    existingGuards.computeIfAbsent(id, ignored -> new HashSet<>()).add(bean.toString());
                    if ("global".equals(id)) {
                        pb.addDefinitionError(LOG.guardWithIdentifierGlobal(bean.toString()));
                    }
                }
            }
        }
    }

    void validate(@Observes AfterDeploymentValidation event) {
        for (Map.Entry<String, Set<String>> entry : existingCircuitBreakerNames.entrySet()) {
            if (entry.getValue().size() > 1) {
                event.addDeploymentProblem(LOG.multipleCircuitBreakersWithTheSameName(
                        entry.getKey(), entry.getValue()));
            }
        }
        // don't clear the `existingCircuitBreakerNames`, they're used later
        // by `CircuitBreakerMaintenance` (see `getExistingCircuitBreakerNames()`)

        for (Map.Entry<String, Set<String>> entry : existingGuards.entrySet()) {
            if (entry.getValue().size() > 1) {
                event.addDeploymentProblem(LOG.multipleGuardsWithTheSameIdentifier(
                        entry.getKey(), entry.getValue()));
            }
        }
        for (String expectedGuard : expectedGuards) {
            if (!existingGuards.containsKey(expectedGuard)) {
                event.addDeploymentProblem(LOG.expectedGuardDoesNotExist(expectedGuard));
            }
        }
        existingGuards.clear();
        expectedGuards.clear();
    }

    private static String getCacheKey(Class<?> beanClass, Method method) {
        return beanClass.getName() + "::" + method.toGenericString();
    }

    FaultToleranceOperation getFaultToleranceOperation(Class<?> beanClass, Method method) {
        return faultToleranceOperations.get(getCacheKey(beanClass, method));
    }

    Set<String> getExistingCircuitBreakerNames() {
        return existingCircuitBreakerNames.keySet();
    }

    private static Optional<String> getImplementationVersion() {
        return AccessController.doPrivileged(new PrivilegedAction<Optional<String>>() {
            @Override
            public Optional<String> run() {
                Properties properties = new Properties();
                try {
                    InputStream resource = this.getClass().getClassLoader()
                            .getResourceAsStream("smallrye-fault-tolerance.properties");
                    if (resource != null) {
                        properties.load(resource);
                        return Optional.ofNullable(properties.getProperty("version"));
                    }
                } catch (IOException e) {
                    LOG.debug("Unable to detect SmallRye Fault Tolerance version");
                }
                return Optional.empty();
            }
        });
    }

    public static class FTInterceptorBindingAnnotatedType<T extends Annotation> implements AnnotatedType<T> {

        public FTInterceptorBindingAnnotatedType(AnnotatedType<T> delegate) {
            this.delegate = delegate;
            annotations = new HashSet<>(delegate.getAnnotations());
            annotations.add(FaultToleranceBinding.Literal.INSTANCE);
        }

        public Class<T> getJavaClass() {
            return delegate.getJavaClass();
        }

        public Set<AnnotatedConstructor<T>> getConstructors() {
            return delegate.getConstructors();
        }

        public Set<AnnotatedMethod<? super T>> getMethods() {
            return delegate.getMethods();
        }

        public Set<AnnotatedField<? super T>> getFields() {
            return delegate.getFields();
        }

        public Type getBaseType() {
            return delegate.getBaseType();
        }

        public Set<Type> getTypeClosure() {
            return delegate.getTypeClosure();
        }

        @SuppressWarnings("unchecked")
        public <S extends Annotation> S getAnnotation(Class<S> annotationType) {
            if (FaultToleranceBinding.class.equals(annotationType)) {
                return (S) FaultToleranceBinding.Literal.INSTANCE;
            }
            return delegate.getAnnotation(annotationType);
        }

        public Set<Annotation> getAnnotations() {
            return annotations;
        }

        public boolean isAnnotationPresent(Class<? extends Annotation> annotationType) {
            return FaultToleranceBinding.class.equals(annotationType) || delegate.isAnnotationPresent(annotationType);
        }

        private AnnotatedType<T> delegate;

        private Set<Annotation> annotations;
    }

    public static class PriorityLiteral extends AnnotationLiteral<Priority> implements Priority {
        private static final long serialVersionUID = 1L;

        private final int value;

        public PriorityLiteral(int value) {
            this.value = value;
        }

        @Override
        public int value() {
            return value;
        }
    }
}

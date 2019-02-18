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

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.annotation.Priority;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AnnotatedConstructor;
import javax.enterprise.inject.spi.AnnotatedField;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.enterprise.inject.spi.ProcessManagedBean;
import javax.enterprise.util.AnnotationLiteral;

import io.smallrye.faulttolerance.metrics.MetricsCollectorFactory;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.Bulkhead;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.jboss.logging.Logger;

import io.smallrye.faulttolerance.config.FaultToleranceOperation;

/**
 * @author Antoine Sabot-Durand
 */
public class HystrixExtension implements Extension {

    private static final Logger LOGGER = Logger.getLogger(HystrixExtension.class);

    /**
     * @see #collectFaultToleranceOperations(ProcessManagedBean)
     */
    private final ConcurrentMap<String, FaultToleranceOperation> faultToleranceOperations = new ConcurrentHashMap<>();

    void registerInterceptorBindings(@Observes BeforeBeanDiscovery bbd, BeanManager bm) {
        LOGGER.info("MicroProfile: Fault Tolerance activated");
        bbd.addInterceptorBinding(new HystrixInterceptorBindingAnnotatedType<>(bm.createAnnotatedType(CircuitBreaker.class)));
        bbd.addInterceptorBinding(new HystrixInterceptorBindingAnnotatedType<>(bm.createAnnotatedType(Retry.class)));
        bbd.addInterceptorBinding(new HystrixInterceptorBindingAnnotatedType<>(bm.createAnnotatedType(Timeout.class)));
        bbd.addInterceptorBinding(new HystrixInterceptorBindingAnnotatedType<>(bm.createAnnotatedType(Asynchronous.class)));
        bbd.addInterceptorBinding(new HystrixInterceptorBindingAnnotatedType<>(bm.createAnnotatedType(Fallback.class)));
        bbd.addInterceptorBinding(new HystrixInterceptorBindingAnnotatedType<>(bm.createAnnotatedType(Bulkhead.class)));

        // Add AnnotatedType for HystrixCommandInterceptor
        // It seems that fraction deployment module cannot be picked up as a CDI bean archive - see also SWARM-1725
        bbd.addAnnotatedType(bm.createAnnotatedType(HystrixCommandInterceptor.class), HystrixCommandInterceptor.class.getName());
        bbd.addAnnotatedType(bm.createAnnotatedType(HystrixInitializer.class), HystrixInitializer.class.getName());
        bbd.addAnnotatedType(bm.createAnnotatedType(DefaultHystrixConcurrencyStrategy.class), DefaultHystrixConcurrencyStrategy.class.getName());
        bbd.addAnnotatedType(bm.createAnnotatedType(DefaultFaultToleranceOperationProvider.class), DefaultFaultToleranceOperationProvider.class.getName());
        bbd.addAnnotatedType(bm.createAnnotatedType(DefaultFallbackHandlerProvider.class), DefaultFallbackHandlerProvider.class.getName());
        bbd.addAnnotatedType(bm.createAnnotatedType(DefaultCommandListenersProvider.class), DefaultCommandListenersProvider.class.getName());
        bbd.addAnnotatedType(bm.createAnnotatedType(MetricsCollectorFactory.class), MetricsCollectorFactory.class.getName());
    }

    void changeInterceptorPriority(@Observes ProcessAnnotatedType<HystrixCommandInterceptor> event) {
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
            FaultToleranceOperation operation = FaultToleranceOperation.of(annotatedMethod);
            if (operation.isLegitimate()) {
                operation.validate();
                LOGGER.debugf("Found %s", operation);
                faultToleranceOperations.put(getCacheKey(annotatedType.getJavaClass(), annotatedMethod.getJavaMember()), operation);
            }
        }
    }

    private static String getCacheKey(Class<?> beanClass, Method method) {
        return beanClass.getName() + "::" + method.toGenericString();
    }

    FaultToleranceOperation getFaultToleranceOperation(Class<?> beanClass, Method method) {
        return faultToleranceOperations.get(getCacheKey(beanClass, method));
    }

    public static class HystrixInterceptorBindingAnnotatedType<T extends Annotation> implements AnnotatedType<T> {

        public HystrixInterceptorBindingAnnotatedType(AnnotatedType<T> delegate) {
            this.delegate = delegate;
            annotations = new HashSet<>(delegate.getAnnotations());
            annotations.add(HystrixCommandBinding.Literal.INSTANCE);
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
            if (HystrixCommandBinding.class.equals(annotationType)) {
                return (S) HystrixCommandBinding.Literal.INSTANCE;
            }
            return delegate.getAnnotation(annotationType);
        }

        public Set<Annotation> getAnnotations() {
            return annotations;
        }

        public boolean isAnnotationPresent(Class<? extends Annotation> annotationType) {
            return HystrixCommandBinding.class.equals(annotationType) || delegate.isAnnotationPresent(annotationType);
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

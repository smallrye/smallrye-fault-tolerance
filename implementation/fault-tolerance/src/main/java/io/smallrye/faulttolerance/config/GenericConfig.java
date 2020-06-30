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
import java.lang.annotation.ElementType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.PrivilegedActionException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import javax.enterprise.inject.spi.AnnotatedMethod;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.faulttolerance.exceptions.FaultToleranceDefinitionException;

/**
 * @author Antoine Sabot-Durand
 */
public abstract class GenericConfig<X extends Annotation> {

    /**
     * This config property key can be used to disable config parameters caching. If disabled, properties are resolved every
     * time a config parameter is needed.
     */
    public static final String CONFIG_PARAMS_CACHE_KEY = "org_wildfly_swarm_microprofile_faulttolerance_configParamsCache";

    GenericConfig(Class<X> annotationType, Class<?> beanClass, Method method) {
        this(beanClass, method, null,
                annotationType, method.isAnnotationPresent(annotationType)
                        ? method.getAnnotation(annotationType)
                        : getAnnotationFromClass(annotationType, beanClass),
                method.isAnnotationPresent(annotationType) ? ElementType.METHOD : ElementType.TYPE);
    }

    GenericConfig(Class<X> annotationType, AnnotatedMethod<?> annotatedMethod) {
        this(annotatedMethod.getDeclaringType().getJavaClass(), annotatedMethod.getJavaMember(), annotatedMethod,
                annotationType, annotatedMethod.isAnnotationPresent(annotationType)
                        ? annotatedMethod.getAnnotation(annotationType)
                        : annotatedMethod.getDeclaringType().getAnnotation(annotationType),
                annotatedMethod.isAnnotationPresent(annotationType) ? ElementType.METHOD : ElementType.TYPE);
    }

    @SuppressWarnings("UnnecessaryThis")
    private GenericConfig(Class<?> beanClass, Method method, AnnotatedMethod<?> annotatedMethod,
            Class<X> annotationType, X annotation, ElementType annotationSource) {
        this.beanClass = beanClass;
        this.method = method;
        this.annotatedMethod = annotatedMethod;
        this.annotationType = annotationType;
        this.annotation = annotation;
        this.annotationSource = annotationSource;
        this.values = getConfig().getOptionalValue(CONFIG_PARAMS_CACHE_KEY, Boolean.class).orElse(true)
                ? new ConcurrentHashMap<>()
                : null;
    }

    @SuppressWarnings("unchecked")
    public <U> U get(String key) {
        if (values != null) {
            return (U) values.computeIfAbsent(key, k -> lookup(k, getKeysToType().get(key)));
        }
        Class<U> expectedType = (Class<U>) getKeysToType().get(key);
        return lookup(key, expectedType);
    }

    @SuppressWarnings("unchecked")
    public <U> U get(String key, Class<U> expectedType) {
        if (values != null) {
            return (U) values.computeIfAbsent(key, k -> lookup(k, expectedType));
        }
        return lookup(key, expectedType);
    }

    /**
     * Note that:
     *
     * <pre>
     * If no annotation matches the specified parameter, the property will be ignored.
     * </pre>
     *
     * @param key
     * @param expectedType
     * @return the configured value
     */
    private <U> U lookup(String key, Class<U> expectedType) {
        Config config = getConfig();
        Optional<U> value;
        if (ElementType.METHOD.equals(annotationSource)) {
            // <classname>/<methodname>/<annotation>/<parameter>
            value = config.getOptionalValue(getConfigKeyForMethod() + key, expectedType);
        } else {
            // <classname>/<annotation>/<parameter>
            value = config.getOptionalValue(getConfigKeyForClass() + key, expectedType);
        }
        if (!value.isPresent()) {
            // <annotation>/<parameter>
            value = config.getOptionalValue(annotationType.getSimpleName() + "/" + key, expectedType);
        }
        // annotation values
        return value.orElseGet(() -> getConfigFromAnnotation(key));
    }

    public abstract void validate();

    @SuppressWarnings("unchecked")
    private <U> U getConfigFromAnnotation(String key) {
        try {
            return (U) SecurityActions.getAnnotationMethod(annotationType, key).invoke(annotation);
        } catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException | IllegalArgumentException
                | PrivilegedActionException e) {
            throw new FaultToleranceDefinitionException(
                    "Member " + key + " on annotation " + annotation.getClass().toString()
                            + " doesn't exist or is not accessible");
        }
    }

    protected String getConfigKeyForMethod() {
        return method.getDeclaringClass().getName() + "/" + method.getName() + "/" + annotationType.getSimpleName() + "/";
    }

    protected String getConfigKeyForClass() {
        return method.getDeclaringClass().getName() + "/" + annotationType.getSimpleName() + "/";
    }

    public String getMethodInfo() {
        return annotatedMethod != null ? annotatedMethod.toString() : method.toGenericString();
    }

    protected static Config getConfig() {
        return ConfigProvider.getConfig();
    }

    private static <A extends Annotation> A getAnnotationFromClass(Class<A> annotationType, Class<?> beanClass) {
        while (beanClass != Object.class) {
            A annotation = beanClass.getAnnotation(annotationType);
            if (annotation != null) {
                return annotation;
            }
            beanClass = beanClass.getSuperclass();
        }
        throw new IllegalStateException(annotationType + " not found on " + beanClass);
    }

    protected abstract Map<String, Class<?>> getKeysToType();

    protected final Class<?> beanClass;

    protected final Method method;

    protected final X annotation;

    protected final Class<X> annotationType;

    // Annotated method is optional
    protected final AnnotatedMethod<?> annotatedMethod;

    protected final ElementType annotationSource;

    private final Map<String, Object> values;

}

package io.smallrye.faulttolerance.defaultmethod;

import static io.smallrye.faulttolerance.core.util.SneakyThrow.sneakyThrow;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.InterceptionType;
import javax.enterprise.inject.spi.Interceptor;
import javax.interceptor.InvocationContext;

// adapted from https://github.com/resteasy/resteasy-microprofile/tree/main/rest-client-base/src/main/java/org/jboss/resteasy/microprofile/client
// which is Apache 2.0 licensed
public class InterceptingInvocationHandler implements InvocationHandler {
    private final Object target;

    private final Map<Method, List<InterceptorInvocation>> interceptorChains;

    public InterceptingInvocationHandler(Class<?> iface, Object target, BeanManager beanManager) {
        this.target = target;

        // this CreationalContext is never released, but that's ok _in a test_
        CreationalContext<?> creationalContext = beanManager.createCreationalContext(null);
        this.interceptorChains = initInterceptorChains(beanManager, creationalContext, iface);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        List<InterceptorInvocation> chain = interceptorChains.get(method);
        if (chain != null) {
            return new InvocationContextImpl(target, method, args, chain).proceed();
        } else {
            try {
                return method.invoke(target, args);
            } catch (InvocationTargetException e) {
                throw e.getCause();
            }
        }
    }

    private static Map<Method, List<InterceptorInvocation>> initInterceptorChains(BeanManager beanManager,
            CreationalContext<?> creationalContext, Class<?> iface) {

        Map<Method, List<InterceptorInvocation>> chains = new HashMap<>();
        Map<Interceptor<?>, Object> interceptorInstances = new HashMap<>();

        List<Annotation> classLevelBindings = getBindings(iface, beanManager);

        for (Method method : iface.getMethods()) {
            if (method.isDefault() || Modifier.isStatic(method.getModifiers())) {
                continue;
            }

            List<Annotation> methodLevelBindings = getBindings(method, beanManager);
            if (!classLevelBindings.isEmpty() || !methodLevelBindings.isEmpty()) {
                Annotation[] interceptorBindings = mergeBindings(methodLevelBindings, classLevelBindings);

                List<Interceptor<?>> interceptors = beanManager.resolveInterceptors(InterceptionType.AROUND_INVOKE,
                        interceptorBindings);
                if (!interceptors.isEmpty()) {
                    List<InterceptorInvocation> chain = new ArrayList<>();
                    for (Interceptor<?> interceptor : interceptors) {
                        chain.add(new InterceptorInvocation((Interceptor<Object>) interceptor,
                                interceptorInstances.computeIfAbsent(interceptor,
                                        i -> beanManager.getReference(i, i.getBeanClass(), creationalContext))));
                    }
                    chains.put(method, chain);
                }
            }
        }

        return chains;
    }

    private static List<Annotation> getBindings(AnnotatedElement annotated, BeanManager beanManager) {
        Annotation[] annotations = annotated.getAnnotations();
        if (annotations.length == 0) {
            return Collections.emptyList();
        }
        List<Annotation> bindings = new ArrayList<>();
        for (Annotation annotation : annotations) {
            if (beanManager.isInterceptorBinding(annotation.annotationType())) {
                bindings.add(annotation);
            }
        }
        return bindings;
    }

    private static Annotation[] mergeBindings(List<Annotation> methodLevel, List<Annotation> classLevel) {
        Set<Class<? extends Annotation>> methodLevelTypes = methodLevel.stream()
                .map(Annotation::annotationType)
                .collect(Collectors.toSet());

        List<Annotation> result = new ArrayList<>(methodLevel);
        for (Annotation classLevelBinding : classLevel) {
            if (!methodLevelTypes.contains(classLevelBinding.annotationType())) {
                result.add(classLevelBinding);
            }
        }
        return result.toArray(new Annotation[0]);
    }

    private static class InvocationContextImpl implements InvocationContext {
        private final Object target;
        private final Method method;
        private Object[] arguments;
        private final Map<String, Object> contextData;

        private final List<InterceptorInvocation> chain;
        private final int position;

        public InvocationContextImpl(Object target, Method method, Object[] arguments,
                List<InterceptorInvocation> chain) {
            this(target, method, arguments, chain, 0);
        }

        private InvocationContextImpl(Object target, Method method, Object[] arguments,
                List<InterceptorInvocation> chain, int position) {
            this.target = target;
            this.method = method;
            this.arguments = arguments;
            this.contextData = new HashMap<>();
            this.chain = chain;
            this.position = position;
        }

        @Override
        public Object proceed() throws Exception {
            try {
                if (position < chain.size()) {
                    InvocationContextImpl ctx = new InvocationContextImpl(target, method, arguments, chain, position + 1);
                    return chain.get(position).invoke(ctx);
                } else {
                    return method.invoke(target, arguments);
                }
            } catch (InvocationTargetException e) {
                throw sneakyThrow(e.getCause());
            }
        }

        @Override
        public Object getTarget() {
            return target;
        }

        @Override
        public Method getMethod() {
            return method;
        }

        @Override
        public Constructor<?> getConstructor() {
            return null;
        }

        @Override
        public Object[] getParameters() throws IllegalStateException {
            return arguments;
        }

        @Override
        public void setParameters(Object[] args) throws IllegalStateException, IllegalArgumentException {
            this.arguments = args;
        }

        @Override
        public Map<String, Object> getContextData() {
            return contextData;
        }

        @Override
        public Object getTimer() {
            return null;
        }
    }

    private static class InterceptorInvocation {
        private final Interceptor<Object> interceptorBean;
        private final Object interceptorInstance;

        public InterceptorInvocation(Interceptor<Object> interceptorBean, Object interceptorInstance) {
            this.interceptorBean = interceptorBean;
            this.interceptorInstance = interceptorInstance;
        }

        public Object invoke(InvocationContext ctx) throws Exception {
            return interceptorBean.intercept(InterceptionType.AROUND_INVOKE, interceptorInstance, ctx);
        }
    }
}

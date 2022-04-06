package io.smallrye.faulttolerance.defaultmethod;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.spi.AfterBeanDiscovery;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.BeforeBeanDiscovery;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.inject.spi.ProcessAnnotatedType;
import jakarta.enterprise.inject.spi.WithAnnotations;

public class InterfaceBasedExtension implements Extension {
    private static final Logger LOG = Logger.getLogger(InterfaceBasedExtension.class.getName());

    private final List<Class<?>> interfaces = new ArrayList<>();

    private void init(@Observes BeforeBeanDiscovery bbd, BeanManager manager) {
        LOG.info(InterfaceBasedExtension.class.getSimpleName() + " initialized");
    }

    public void rememberInterfaces(@Observes @WithAnnotations(RegisterInterfaceBased.class) ProcessAnnotatedType<?> pat) {
        Class<?> clazz = pat.getAnnotatedType().getJavaClass();
        if (clazz.isInterface()) {
            interfaces.add(clazz);
            LOG.info("Found " + clazz);
            pat.veto();
        } else {
            throw new IllegalArgumentException("Must be an interface: " + clazz);
        }
    }

    public void registerBeans(@Observes AfterBeanDiscovery abd, BeanManager beanManager) {
        for (Class<?> iface : interfaces) {
            abd.addBean()
                    .beanClass(iface)
                    .types(iface)
                    .scope(Dependent.class)
                    .qualifiers(Default.Literal.INSTANCE, Any.Literal.INSTANCE, InterfaceBased.Literal.INSTANCE)
                    .createWith(ctx -> {
                        Object target = Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
                                new Class[] { iface }, InterfaceBasedExtension::invoke);
                        return Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
                                new Class[] { iface }, new InterceptingInvocationHandler(iface, target, beanManager));
                    });
            LOG.info("Registered bean for " + iface);
        }
    }

    private static Object invoke(Object proxy, Method method, Object[] args) {
        throw new IllegalArgumentException("Always an exception");
    }
}

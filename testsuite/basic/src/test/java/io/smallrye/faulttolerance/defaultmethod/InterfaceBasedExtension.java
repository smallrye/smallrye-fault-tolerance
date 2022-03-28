package io.smallrye.faulttolerance.defaultmethod;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.enterprise.context.Dependent;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.enterprise.inject.spi.WithAnnotations;

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

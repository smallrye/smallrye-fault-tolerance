package io.smallrye.faulttolerance.util;

import static io.smallrye.faulttolerance.core.util.SneakyThrow.sneakyThrow;
import static org.junit.platform.commons.support.AnnotationSupport.findAnnotation;

import java.lang.reflect.Method;
import java.util.Optional;

import org.jboss.weld.junit5.auto.WeldJunit5AutoExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;

/**
 * Poor replacement of Arquillian's {@code ShouldThrowException}.
 * Most likely doesn't cover all possible code paths, but works fine for the few cases in this test suite.
 */
public class WeldWithFaultToleranceExtension extends WeldJunit5AutoExtension implements InvocationInterceptor {
    private static final String SKIP_TEST_INVOCATIONS = "WeldWithFaultToleranceExtension.SKIP_TEST_INVOCATIONS";

    private static ExtensionContext classContext(ExtensionContext extensionContext) {
        if (extensionContext.getElement().isPresent() && extensionContext.getElement().get() instanceof Class) {
            return extensionContext;
        }

        if (extensionContext.getParent().isPresent()) {
            return classContext(extensionContext.getParent().get());
        }

        throw new IllegalStateException("Can't find ExtensionContext for test class");
    }

    private static ExtensionContext.Namespace getNamespace(ExtensionContext extensionContext) {
        return ExtensionContext.Namespace.create(WeldWithFaultToleranceExtension.class,
                extensionContext.getRequiredTestClass());
    }

    @Override
    public void beforeAll(ExtensionContext extensionContext) {
        try {
            super.beforeAll(extensionContext);
        } catch (Exception e) {
            handle(e, extensionContext);
        }
    }

    @Override
    public void beforeEach(ExtensionContext extensionContext) {
        try {
            super.beforeEach(extensionContext);
        } catch (Exception e) {
            handle(e, extensionContext);
        }
    }

    @Override
    public void afterAll(ExtensionContext extensionContext) {
        super.afterAll(extensionContext);

        Optional<ExpectedDeploymentException> expectedException = findAnnotation(extensionContext.getRequiredTestClass(),
                ExpectedDeploymentException.class);
        if (expectedException.isPresent() && !shouldSkipTestInvocations(extensionContext)) {
            Assertions.fail("Exception " + expectedException.get().value().getName() + " was expected, but wasn't thrown");
        }
    }

    private void handle(Exception exception, ExtensionContext extensionContext) {
        Optional<ExpectedDeploymentException> expectedException = findAnnotation(extensionContext.getRequiredTestClass(),
                ExpectedDeploymentException.class);
        if (expectedException.isPresent()) {
            Class<?> expectedExceptionType = expectedException.get().value();
            Throwable probedException = exception;
            while (probedException != null) {
                if (expectedExceptionType.isAssignableFrom(probedException.getClass())) {
                    classContext(extensionContext).getStore(getNamespace(extensionContext))
                            .put(SKIP_TEST_INVOCATIONS, Boolean.TRUE);
                    return;
                }
                probedException = probedException.getCause();
            }
        }

        throw sneakyThrow(exception);
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
            throws ParameterResolutionException {
        if (shouldSkipTestInvocations(extensionContext)) {
            return true;
        }
        return super.supportsParameter(parameterContext, extensionContext);
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
            throws ParameterResolutionException {
        if (shouldSkipTestInvocations(extensionContext)) {
            return null;
        }
        return super.resolveParameter(parameterContext, extensionContext);
    }

    @Override
    public void interceptTestMethod(Invocation<Void> invocation, ReflectiveInvocationContext<Method> invocationContext,
            ExtensionContext extensionContext) throws Throwable {
        if (shouldSkipTestInvocations(extensionContext)) {
            invocation.skip();
        } else {
            invocation.proceed();
        }
    }

    private boolean shouldSkipTestInvocations(ExtensionContext extensionContext) {
        return classContext(extensionContext).getStore(getNamespace(extensionContext))
                .getOrDefault(SKIP_TEST_INVOCATIONS, Boolean.class, false);
    }
}

package io.smallrye.faulttolerance.config;

import java.lang.reflect.Method;

import org.eclipse.microprofile.faulttolerance.exceptions.FaultToleranceDefinitionException;

import io.smallrye.faulttolerance.api.BeforeRetry;
import io.smallrye.faulttolerance.autoconfig.AutoConfig;
import io.smallrye.faulttolerance.autoconfig.Config;
import io.smallrye.faulttolerance.internal.BeforeRetryMethod;
import io.smallrye.faulttolerance.internal.InterceptionPoint;

@AutoConfig
public interface BeforeRetryConfig extends BeforeRetry, Config {
    default void validate() {
        final String INVALID_BEFORE_RETRY_ON = "Invalid @BeforeRetry on ";

        Method guardedMethod;
        try {
            guardedMethod = method().reflect();
        } catch (NoSuchMethodException e) {
            throw new FaultToleranceDefinitionException(e);
        }

        if (!"".equals(methodName())) {
            if (!BeforeRetry.DEFAULT.class.equals(value())) {
                throw new FaultToleranceDefinitionException(INVALID_BEFORE_RETRY_ON + method()
                        + ": before retry handler class and before retry method can't be specified both at the same time");
            }

            BeforeRetryMethod beforeRetryMethod = BeforeRetryMethod.find(
                    new InterceptionPoint(beanClass(), guardedMethod), methodName());
            if (beforeRetryMethod == null) {
                throw new FaultToleranceDefinitionException(INVALID_BEFORE_RETRY_ON + method()
                        + ": can't find before retry method '" + methodName()
                        + "' with no parameter and return type of void");
            }
        }
    }
}

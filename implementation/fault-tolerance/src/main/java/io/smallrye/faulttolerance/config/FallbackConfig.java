package io.smallrye.faulttolerance.config;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.FallbackHandler;
import org.eclipse.microprofile.faulttolerance.exceptions.FaultToleranceDefinitionException;

import io.smallrye.faulttolerance.autoconfig.AutoConfig;
import io.smallrye.faulttolerance.autoconfig.Config;

@AutoConfig
public interface FallbackConfig extends Fallback, Config {
    @Override
    default void validate() {
        final String INVALID_FALLBACK_ON = "Invalid @Fallback on ";

        Method guardedMethod;
        try {
            guardedMethod = method().reflect();
        } catch (NoSuchMethodException e) {
            throw new FaultToleranceDefinitionException(e);
        }

        if (!"".equals(fallbackMethod()) && !DEFAULT.class.equals(value())) {
            throw new FaultToleranceDefinitionException(INVALID_FALLBACK_ON + method()
                    + ": fallback handler class and fallback method can't be specified both at the same time");
        }

        if (!Fallback.DEFAULT.class.equals(value())) {
            Type fallbackType = null;
            for (Type genericInterface : value().getGenericInterfaces()) {
                if (genericInterface instanceof ParameterizedType) {
                    ParameterizedType parameterizedType = (ParameterizedType) genericInterface;
                    if (parameterizedType.getRawType().equals(FallbackHandler.class)) {
                        fallbackType = parameterizedType.getActualTypeArguments()[0];
                        break;
                    }
                }
            }

            Type boxedReturnType = FallbackValidation.box(guardedMethod.getGenericReturnType());

            if (KotlinSupport.isSuspendingFunction(guardedMethod)) {
                boxedReturnType = KotlinSupport.getSuspendingFunctionResultType(guardedMethod);
            }

            if (!boxedReturnType.equals(fallbackType)) {
                throw new FaultToleranceDefinitionException(INVALID_FALLBACK_ON + method()
                        + ": fallback handler's type " + fallbackType + " is not the same as method's return type");
            }
        }
    }
}

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

import com.netflix.hystrix.HystrixCircuitBreaker;
import com.netflix.hystrix.exception.HystrixTimeoutException;
import io.smallrye.faulttolerance.config.FaultToleranceOperation;
import org.eclipse.microprofile.faulttolerance.exceptions.BulkheadException;
import org.eclipse.microprofile.faulttolerance.exceptions.CircuitBreakerOpenException;
import org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import static io.smallrye.faulttolerance.config.CircuitBreakerConfig.FAIL_ON;

/**
 * @author Antoine Sabot-Durand
 * @author Martin Kouba
 */
public class SimpleCommand extends BasicCommand {

    private AtomicBoolean canceled = new AtomicBoolean(false);
    private Thread executionThread;

    public static String getCommandKey(Method method) {
        StringBuilder builder = new StringBuilder();
        builder.append(method.getDeclaringClass().getName().replace(".", "_"));
        builder.append("#");
        builder.append(method.getName());
        builder.append("(");
        Type[] params = method.getGenericParameterTypes();
        for (int j = 0; j < params.length; j++) {
            builder.append(params[j].getTypeName());
            if (j < (params.length - 1)) {
                builder.append(',');
            }
        }
        builder.append(")");
        return builder.toString();
    }

    /**
     *
     * @param setter Hystrix command setter
     * @param ctx Execution context
     * @param fallback Fallback
     * @param operation Fault tolerance operation
     * @param listeners Command listeners
     */
    protected SimpleCommand(Setter setter,
                            ExecutionContextWithInvocationContext ctx,
                            Supplier<Object> fallback,
                            FaultToleranceOperation operation,
                            Iterable<CommandListener> listeners,
                            RetryContext retryContext) {
        super(setter);
        this.ctx = ctx;
        this.fallback = fallback;
        this.operation = operation;
        this.listeners = listeners;
        this.retryContext = retryContext;
    }

    @Override
    protected Object run() throws Exception {
        executionThread = Thread.currentThread();
        if (canceled.get()) {
            return null;
        }
        if (listeners == null) {
            return ctx.proceed();
        }
        try {
            for (CommandListener listener : listeners) {
                listener.beforeExecution(operation);
            }
            return ctx.proceed();
        } finally {
            for (CommandListener listener : listeners) {
                listener.afterExecution(operation);
            }
        }
    }

    @Override
    protected Object getFallback() {
        if (fallback == null) {
            return super.getFallback();
        }
        Throwable failure = getFailedExecutionException();
        if (failure != null && operation.hasCircuitBreaker() && !isFailureAssignableFromAnyFailureException(failure)) {
            // Command failed but the fallback should not be used
            throw new FailureNotHandledException(failure);
        }
        if (failure == null) {
            failure = translateException();
        }
        if (retryContext == null || !retryContext.shouldRetryOn(failure)) {
            return fallback.get();
        } else {
            return super.getFallback();
        }
    }

    // TODO: improve this, see: https://github.com/smallrye/smallrye-fault-tolerance/issues/52
    private Throwable translateException() {
        Exception e = executionResult.getExecutionException();
        if (e instanceof HystrixTimeoutException) {
            return new TimeoutException(e);
        }

        switch (e.getMessage()) {
            case "could not acquire a semaphore for execution":
                return new BulkheadException(e);
            case "Hystrix circuit short-circuited and is OPEN":
                return new CircuitBreakerOpenException(e);
            default:
                return e;
        }
    }

    private boolean isFailureAssignableFromAnyFailureException(Throwable failure) {
        Class<?>[] exceptions = operation.getCircuitBreaker().<Class<?>[]> get(FAIL_ON);
        for (Class<?> exception : exceptions) {
            if (exception.isAssignableFrom(failure.getClass())) {
                return true;
            }
        }
        return false;
    }

    @Override
     void setFailure(Throwable f) {
        ctx.setFailure(f);
    }

    private final FaultToleranceOperation operation;

    private final Supplier<Object> fallback;

    private final ExecutionContextWithInvocationContext ctx;

    private final Iterable<CommandListener> listeners;

    private final RetryContext retryContext;

    @Override
    FaultToleranceOperation getOperation() {
        return operation;
    }

    public HystrixCircuitBreaker getCircuitBreaker() {
        return circuitBreaker;
    }

    public void cancel(boolean mayInterrupt) {
        canceled.set(true);
        if (mayInterrupt) {
            if (executionThread != null) {
                executionThread.interrupt();
            }
        }
    }
}

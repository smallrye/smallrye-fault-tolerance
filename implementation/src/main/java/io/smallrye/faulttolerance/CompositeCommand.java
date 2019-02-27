/*
 * Copyright 2018 Red Hat, Inc, and individual contributors.
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

import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import io.smallrye.faulttolerance.metrics.MetricNames;
import io.smallrye.faulttolerance.metrics.MetricsCollectorFactory;
import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.MetricType;

import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixCommandProperties;
import com.netflix.hystrix.HystrixThreadPoolKey;
import com.netflix.hystrix.HystrixThreadPoolProperties;

import io.smallrye.faulttolerance.config.FaultToleranceOperation;

/**
 * This command is used to wrap any {@link Asynchronous} operation.
 *
 * @author Martin Kouba
 */
public class CompositeCommand extends BasicCommand {

    public static Future<Object> createAndQueue(Callable<Object> callable, FaultToleranceOperation operation,
            RetryContext retryContext, ExecutionContextWithInvocationContext ctx, MetricRegistry registry,
            boolean timeoutEnabled) {
        return new CompositeCommand(callable, operation, retryContext, ctx, registry, timeoutEnabled).queue();
    }

    @Override
    void setFailure(Throwable f) {
        ctx.setFailure(f);
    }

    @Override
    FaultToleranceOperation getOperation() {
        return operation;
    }

    private final Callable<Object> callable;

    private final ExecutionContextWithInvocationContext ctx;

    private final FaultToleranceOperation operation;

    private final RetryContext retryContext;

    private final MetricRegistry registry;


    /**
     *
     * @param callable Asynchronous operation
     * @param operation Fault tolerance operation
     */
    protected CompositeCommand(Callable<Object> callable, FaultToleranceOperation operation, RetryContext retryContext,
            ExecutionContextWithInvocationContext ctx, MetricRegistry registry, boolean timeoutEnabled) {
        super(initSetter(operation, timeoutEnabled));
        this.operation = operation;
        this.callable = callable;
        this.retryContext = retryContext;
        this.ctx = ctx;
        this.registry = registry;
    }

    @Override
    protected Object run() throws Exception {
        String metricsPrefix = MetricNames.metricsPrefix(operation.getMethod());

        if (retryContext == null) {
            return callable.call();
        }

        // the retry metrics collection here mirrors the logic in HystrixCommandInterceptor.executeCommand
        // and MetricsCollectorFactory.MetricsCollectorImpl.beforeExecute/afterSuccess/onError
        while (true) {
            try {
                if (registry != null && retryContext.hasBeenRetried()) {
                    counterOf(metricsPrefix + MetricNames.RETRY_RETRIES_TOTAL).inc();
                }
                Object result = callable.call();
                if (registry != null) {
                    if (retryContext.hasBeenRetried()) {
                        counterOf(metricsPrefix + MetricNames.RETRY_CALLS_SUCCEEDED_RETRIED_TOTAL).inc();
                    } else {
                        counterOf(metricsPrefix + MetricNames.RETRY_CALLS_SUCCEEDED_NOT_RETRIED_TOTAL).inc();
                    }
                }
                return result;
            } catch (Throwable e) {
                if (retryContext.shouldRetry()) {
                    Exception shouldRetry = retryContext.nextRetry(e);
                    if (shouldRetry != null) {
                        throw shouldRetry;
                    }
                } else {
                    if (registry != null) {
                        counterOf(metricsPrefix + MetricNames.RETRY_CALLS_FAILED_TOTAL).inc();
                    }
                    throw e;
                }
            }
        }
    }

    // needs to be identical to CompositeObservableCommand.initSetter
    private static Setter initSetter(FaultToleranceOperation operation, boolean timeoutEnabled) {
        HystrixCommandKey commandKey = hystrixCommandKey(operation);

        return Setter
                .withGroupKey(hystrixCommandGroupKey())
                .andCommandKey(commandKey)
                .andCommandPropertiesDefaults(HystrixCommandProperties.Setter()
                        .withExecutionIsolationStrategy(HystrixCommandProperties.ExecutionIsolationStrategy.THREAD)
                        .withFallbackEnabled(false)
                        .withCircuitBreakerEnabled(false)
                        .withExecutionTimeoutEnabled(timeoutEnabled))
                // We use a dedicated thread pool for each async operation
                .andThreadPoolKey(HystrixThreadPoolKey.Factory.asKey(commandKey.name()))
                .andThreadPoolPropertiesDefaults(HystrixThreadPoolProperties.Setter()
                        .withAllowMaximumSizeToDivergeFromCoreSize(true));
    }

    // duplicate of MetricsCollectorFactory.MetricsCollectorImpl.counterOf
    private Counter counterOf(String name) {
        Counter counter = registry.getCounters().get(name);
        if (counter == null) {
            synchronized (operation) {
                counter = registry.getCounters().get(name);
                if (counter == null) {
                    counter = registry.counter(MetricsCollectorFactory.metadataOf(name, MetricType.COUNTER));
                }
            }
        }
        return counter;
    }

    static HystrixCommandGroupKey hystrixCommandGroupKey() {
        return HystrixCommandGroupKey.Factory.asKey("CompositeCommandGroup");
    }

    static HystrixCommandKey hystrixCommandKey(FaultToleranceOperation operation) {
        return HystrixCommandKey.Factory.asKey(CompositeCommand.class.getSimpleName()
                + "#" + SimpleCommand.getCommandKey(operation.getMethod()));
    }
}

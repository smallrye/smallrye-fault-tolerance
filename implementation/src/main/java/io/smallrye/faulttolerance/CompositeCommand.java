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

import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.metrics.Histogram;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.MetricType;

import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixCommandProperties;
import com.netflix.hystrix.HystrixThreadPoolKey;
import com.netflix.hystrix.HystrixThreadPoolProperties;

import io.smallrye.faulttolerance.config.FaultToleranceOperation;
import org.jboss.logging.Logger;

/**
 * This command is used to wrap any {@link Asynchronous} operation.
 *
 * @author Martin Kouba
 */
public class CompositeCommand extends BasicCommand {

    private static final Logger LOGGER = Logger.getLogger(DefaultHystrixConcurrencyStrategy.class);

    public static Future<Object> createAndQueue(Callable<Object> callable, FaultToleranceOperation operation,
            RetryContext retryContext, ExecutionContextWithInvocationContext ctx, MetricRegistry registry) {
        return new CompositeCommand(callable, operation, retryContext, ctx, registry).queue();
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

    private final long queuedAt;

    /**
     *
     * @param callable Asynchronous operation
     * @param operation Fault tolerance operation
     */
    protected CompositeCommand(Callable<Object> callable, FaultToleranceOperation operation, RetryContext retryContext,
            ExecutionContextWithInvocationContext ctx, MetricRegistry registry) {
        super(initSetter(operation));
        this.callable = callable;
        this.operation = operation;
        this.retryContext = retryContext;
        this.ctx = ctx;
        this.registry = registry;
        this.queuedAt = System.nanoTime();
    }

    @Override
    protected Object run() throws Exception {
        try {
            if (registry != null && operation.hasBulkhead()) {
                // TODO: in fact, we do not record the time spent in the queue but the time between command creation and command execution
                histogramOf(MetricNames.metricsPrefix(operation.getMethod()) + MetricNames.BULKHEAD_WAITING_DURATION).update(System.nanoTime() - queuedAt);
            }
        } catch (Exception any) {
            LOGGER.warn("Failed to update metrics", any);
        }

        if (retryContext == null) {
            return callable.call();
        }

        while (true) {
            try {
                return callable.call();
            } catch (Throwable e) {
                if (retryContext.shouldRetry()) {
                    Exception shouldRetry = retryContext.nextRetry(e);
                    if (shouldRetry != null) {
                        throw shouldRetry;
                    }
                } else {
                    throw e;
                }
            }
        }
    }

    // needs to be identical to CompositeObservableCommand.initSetter
    private static Setter initSetter(FaultToleranceOperation operation) {
        HystrixCommandKey commandKey = hystrixCommandKey(operation);

        return Setter
                .withGroupKey(hystrixCommandGroupKey())
                .andCommandKey(commandKey)
                .andCommandPropertiesDefaults(HystrixCommandProperties.Setter()
                        .withExecutionIsolationStrategy(HystrixCommandProperties.ExecutionIsolationStrategy.THREAD)
                        .withFallbackEnabled(false)
                        .withCircuitBreakerEnabled(false)
                        .withExecutionTimeoutEnabled(shouldEnableTimeout()))
                // We use a dedicated thread pool for each async operation
                .andThreadPoolKey(HystrixThreadPoolKey.Factory.asKey(commandKey.name()))
                .andThreadPoolPropertiesDefaults(HystrixThreadPoolProperties.Setter()
                        .withAllowMaximumSizeToDivergeFromCoreSize(true));
    }

    private Histogram histogramOf(String name) {
        Histogram histogram = registry.getHistograms().get(name);
        if (histogram == null) {
            synchronized (operation) {
                histogram = registry.getHistograms().get(name);
                if (histogram == null) {
                    histogram = registry.histogram(MetricsCollectorFactory.metadataOf(name, MetricType.HISTOGRAM));
                }
            }
        }
        return histogram;
    }

    static HystrixCommandGroupKey hystrixCommandGroupKey() {
        return HystrixCommandGroupKey.Factory.asKey("CompositeCommandGroup");
    }

    static HystrixCommandKey hystrixCommandKey(FaultToleranceOperation operation) {
        return HystrixCommandKey.Factory.asKey(CompositeCommand.class.getSimpleName()
                + "#" + SimpleCommand.getCommandKey(operation.getMethod()));
    }

    static boolean shouldEnableTimeout() {
        return System.getProperty("smallrye.hystrix.async.timeout.enabled") != null;
    }
}

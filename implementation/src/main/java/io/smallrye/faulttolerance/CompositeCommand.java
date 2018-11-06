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

/**
 * This command is used to wrap any {@link Asynchronous} operation.
 *
 * @author Martin Kouba
 */
public class CompositeCommand extends BasicCommand {

    public static Future<Object> createAndQueue(Callable<Object> callable, FaultToleranceOperation operation, ExecutionContextWithInvocationContext ctx,
            MetricRegistry registry) {
        return new CompositeCommand(callable, operation, ctx, registry).queue();
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

    private final MetricRegistry registry;

    private final long queuedAt;

    /**
     *
     * @param callable Asynchronous operation
     * @param operation Fault tolerance operation
     */
    protected CompositeCommand(Callable<Object> callable, FaultToleranceOperation operation, ExecutionContextWithInvocationContext ctx,
            MetricRegistry registry) {
        super(initSetter(operation));
        this.operation = operation;
        this.callable = callable;
        this.ctx = ctx;
        this.registry = registry;
        this.queuedAt = System.nanoTime();
    }

    @Override
    protected Object run() throws Exception {
        if (registry != null && operation.hasBulkhead()) {
            // TODO: in fact, we do not record the time spent in the queue but the time between command creation and command execution
            histogramOf(MetricNames.metricsPrefix(operation.getMethod()) + MetricNames.BULKHEAD_WAITING_DURATION).update(System.nanoTime() - queuedAt);
        }
        return callable.call();
    }

    private static Setter initSetter(FaultToleranceOperation operation) {
        HystrixCommandProperties.Setter properties = HystrixCommandProperties.Setter();
        HystrixCommandKey commandKey = HystrixCommandKey.Factory
                .asKey(CompositeCommand.class.getSimpleName() + "#" + SimpleCommand.getCommandKey(operation.getMethod()));

        properties.withExecutionIsolationStrategy(HystrixCommandProperties.ExecutionIsolationStrategy.THREAD);
        properties.withFallbackEnabled(false);
        properties.withCircuitBreakerEnabled(false);

        Setter setter = Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey("CompositeCommandGroup")).andCommandKey(commandKey)
                .andCommandPropertiesDefaults(properties);

        // We use a dedicated thread pool for each async operation
        setter.andThreadPoolKey(HystrixThreadPoolKey.Factory.asKey(commandKey.name()));
        HystrixThreadPoolProperties.Setter threadPoolSetter = HystrixThreadPoolProperties.Setter();
        threadPoolSetter.withAllowMaximumSizeToDivergeFromCoreSize(true);
        setter.andThreadPoolPropertiesDefaults(threadPoolSetter);

        return setter;
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

}

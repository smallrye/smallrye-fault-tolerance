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

import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixCommandProperties;
import com.netflix.hystrix.HystrixObservableCommand;
import com.netflix.hystrix.HystrixThreadPoolKey;
import com.netflix.hystrix.HystrixThreadPoolProperties;
import io.smallrye.faulttolerance.config.FaultToleranceOperation;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.Histogram;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.MetricType;
import org.jboss.logging.Logger;
import rx.Observable;

import java.lang.reflect.Field;
import java.security.PrivilegedActionException;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionStage;

/**
 *
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 * <br>
 * Date: 2/7/19
 */
public class CompositeObservableCommand extends HystrixObservableCommand {

    private static final Logger LOGGER = Logger.getLogger(DefaultHystrixConcurrencyStrategy.class);
    
    public static HystrixObservableCommand create(Callable<? extends CompletionStage<?>> callable,
                                                     FaultToleranceOperation operation,
                                                     RetryContext retryContext,
                                                     ExecutionContextWithInvocationContext ctx,
                                                     MetricRegistry registry) {
        return new CompositeObservableCommand(callable, operation, retryContext, ctx, registry);
    }

    private final Callable<? extends CompletionStage> callable;
    private final ExecutionContextWithInvocationContext ctx;
    private final FaultToleranceOperation operation;
    private final RetryContext retryContext;
    private final MetricRegistry registry;
    private final long queuedAt;


    protected CompositeObservableCommand(Callable<? extends CompletionStage> callable,
                                         FaultToleranceOperation operation,
                                         RetryContext retryContext,
                                         ExecutionContextWithInvocationContext ctx,
                                         MetricRegistry registry) {
        super(initSetter(operation));
        this.callable = callable;
        this.ctx = ctx;
        this.operation = operation;
        this.retryContext = retryContext;
        this.registry = registry;
        this.queuedAt = System.nanoTime();
    }

    @Override
    protected Observable construct() {
        String metricsPrefix = MetricNames.metricsPrefix(operation.getMethod());

        try {
            if (registry != null && operation.hasBulkhead()) {
                // TODO: in fact, we do not record the time spent in the queue but the time between command creation and command execution
                histogramOf(metricsPrefix + MetricNames.BULKHEAD_WAITING_DURATION).update(System.nanoTime() - queuedAt);
            }
        } catch (Exception any) {
            LOGGER.warn("Failed to update metrics", any);
        }

        // the retry metrics collection here mirrors the logic in HystrixCommandInterceptor.executeCommand
        // and MetricsCollectorFactory.MetricsCollectorImpl.beforeExecute/afterSuccess/onError
        Observable<Object> observable = Observable.create(
                subscriber -> {
                    try {
                        if (retryContext != null && retryContext.hasBeenRetried()) {
                            counterOf(metricsPrefix + MetricNames.RETRY_RETRIES_TOTAL).inc();
                        }
                        CompletionStage<?> stage = callable.call();
                        if (stage == null) {
                            subscriber.onError(new NullPointerException("A method that should return a CompletionStage returned null"));
                        } else {
                            stage.whenComplete(
                                    (value, error) -> {
                                        if (error == null) {
                                            if (retryContext != null) {
                                                if (retryContext.hasBeenRetried()) {
                                                    counterOf(metricsPrefix + MetricNames.RETRY_CALLS_SUCCEEDED_RETRIED_TOTAL).inc();
                                                } else {
                                                    counterOf(metricsPrefix + MetricNames.RETRY_CALLS_SUCCEEDED_NOT_RETRIED_TOTAL).inc();
                                                }
                                            }

                                            subscriber.onNext(value);
                                            subscriber.onCompleted();
                                        } else {
                                            subscriber.onError(error);
                                        }
                                    }
                            );
                        }
                    } catch (Exception e) {
                        subscriber.onError(e);
                    }
                }
        );

        if (retryContext != null) {
            return observable.retryWhen(attempts -> {
                return attempts.flatMap((Throwable error) -> {
                    if (retryContext.shouldRetry()) {
                        Exception shouldRetry = null;
                        try {
                            shouldRetry = retryContext.nextRetry(error);
                        } catch (Throwable e) {
                            return Observable.error(e);
                        }
                        if (shouldRetry != null) {
                            return Observable.error(shouldRetry);
                        } else {
                            return Observable.just(""); // the value here doesn't matter, it's just a signal to retry
                        }
                    } else {
                        counterOf(metricsPrefix + MetricNames.RETRY_CALLS_FAILED_TOTAL).inc();
                        return Observable.error(error);
                    }
                });
            });
        } else {
            return observable;
        }
    }

    // needs to be identical to CompositeCommand.initSetter
    private static Setter initSetter(FaultToleranceOperation operation) {
        HystrixCommandKey commandKey = CompositeCommand.hystrixCommandKey(operation);

        Setter result = Setter
                .withGroupKey(CompositeCommand.hystrixCommandGroupKey())
                .andCommandKey(commandKey)
                .andCommandPropertiesDefaults(HystrixCommandProperties.Setter()
                        .withExecutionIsolationStrategy(HystrixCommandProperties.ExecutionIsolationStrategy.THREAD)
                        .withFallbackEnabled(false)
                        .withCircuitBreakerEnabled(false)
                        .withExecutionTimeoutEnabled(CompositeCommand.shouldEnableTimeout()));

        try {
            // unfortunately, Hystrix doesn't expose the API to set these, even though everything else is there
            Field threadPoolKey = SecurityActions.getDeclaredField(Setter.class, "threadPoolKey");
            SecurityActions.setAccessible(threadPoolKey);
            threadPoolKey.set(result, HystrixThreadPoolKey.Factory.asKey(commandKey.name()));

            Field threadPoolPropertiesDefaults = SecurityActions.getDeclaredField(Setter.class, "threadPoolPropertiesDefaults");
            SecurityActions.setAccessible(threadPoolPropertiesDefaults);
            threadPoolPropertiesDefaults.set(result, HystrixThreadPoolProperties.Setter()
                    .withAllowMaximumSizeToDivergeFromCoreSize(true));
        } catch (ReflectiveOperationException | PrivilegedActionException e) {
            // oh well...
        }

        return result;
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

    // duplicate of MetricsCollectorFactory.MetricsCollectorImpl.histogramOf
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

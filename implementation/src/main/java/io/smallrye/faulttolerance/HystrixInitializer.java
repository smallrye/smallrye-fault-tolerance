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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Initialized;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.jboss.logging.Logger;

import com.netflix.hystrix.Hystrix;
import com.netflix.hystrix.strategy.HystrixPlugins;
import com.netflix.hystrix.strategy.concurrency.HystrixConcurrencyStrategy;

/**
 * This component configures Hystrix to use a specific {@link HystrixConcurrencyStrategy}.
 *
 * @author Martin Kouba
 */
@ApplicationScoped
public class HystrixInitializer {

    private static final Logger LOGGER = Logger.getLogger(HystrixInitializer.class);

    @Inject
    Instance<HystrixConcurrencyStrategy> instance;

    // Initialize eagerly
    void init(@Observes @Initialized(ApplicationScoped.class) Object event) {
    }

    @PostConstruct
    void onStartup() {
        LOGGER.info("### Init Hystrix ###");
        HystrixConcurrencyStrategy strategy = instance.get();
        LOGGER.info("Hystrix concurrency strategy used: " + strategy.getClass().getSimpleName());
        HystrixPlugins.getInstance().registerConcurrencyStrategy(strategy);
        HystrixPlugins.getInstance().registerCommandExecutionHook(new FaultToleranceCommandExecutionHook());
    }

    @PreDestroy
    void onShutdown() {
        LOGGER.info("### Reset Hystrix ###");
        Hystrix.reset(1, TimeUnit.SECONDS);
    }
}

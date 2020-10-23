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
package io.smallrye.faulttolerance.bulkhead.reject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.inject.Inject;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.util.FaultToleranceBasicTest;

@FaultToleranceBasicTest
public class BulkheadFallbackRejectTest {
    @Inject
    PingService pingService;

    static final int QUEUE_SIZE = 20;

    @Test
    public void testFallbackNotRejected() throws InterruptedException, ExecutionException {
        ExecutorService executorService = Executors.newFixedThreadPool(QUEUE_SIZE);
        try {
            List<Callable<String>> tasks = new ArrayList<>();
            for (int i = 1; i <= QUEUE_SIZE; i++) {
                tasks.add(() -> pingService.ping());
            }
            List<Future<String>> futures = executorService.invokeAll(tasks);
            for (Future<String> future : futures)
                Assertions.assertThat(future.get()).isIn("fallback", "pong");
        } finally {
            executorService.shutdown();
        }
    }

}

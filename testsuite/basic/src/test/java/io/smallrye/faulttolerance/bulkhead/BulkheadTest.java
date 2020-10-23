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
package io.smallrye.faulttolerance.bulkhead;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.eclipse.microprofile.faulttolerance.exceptions.BulkheadException;
import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.util.FaultToleranceBasicTest;

@FaultToleranceBasicTest
public class BulkheadTest {
    static final int QUEUE_SIZE = 3;

    @Test
    public void testWaitingQueue(PingService pingService) throws InterruptedException, ExecutionException {
        int loop = QUEUE_SIZE * 2;
        CountDownLatch startLatch = new CountDownLatch(QUEUE_SIZE);
        CountDownLatch endLatch = new CountDownLatch(1);
        List<Future<String>> futures = new ArrayList<>();
        for (int i = 0; i < loop; i++) {
            futures.add(pingService.ping(startLatch, endLatch));
        }
        assertThat(startLatch.await(5000L, TimeUnit.MILLISECONDS)).isTrue();
        Thread.sleep(500L);
        // Next invocation should not make it due to BulkheadException
        assertThatThrownBy(() -> {
            pingService.ping(null, null).get();
        }).as("The call finished successfully but BulkheadException was expected to be thrown")
                .isExactlyInstanceOf(ExecutionException.class)
                .hasCauseExactlyInstanceOf(BulkheadException.class);
        endLatch.countDown();
        for (int i = 0; i < loop; i++) {
            assertThat(futures.get(i)).isNotCancelled();
            assertThat(futures.get(i).get())
                    .as("the content check failed for future: " + futures.get(i))
                    .isEqualTo("pong");
        }
    }

}

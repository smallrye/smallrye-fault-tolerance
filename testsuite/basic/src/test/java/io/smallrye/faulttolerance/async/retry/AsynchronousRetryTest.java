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
package io.smallrye.faulttolerance.async.retry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.util.FaultToleranceBasicTest;

@FaultToleranceBasicTest
public class AsynchronousRetryTest {
    @Test
    public void testAsyncRetrySuccess(AsyncHelloService helloService)
            throws IOException, InterruptedException, ExecutionException {
        AsyncHelloService.COUNTER.set(0);
        assertThat(helloService.retry(AsyncHelloService.Result.SUCCESS).get()).isEqualTo("Hello");
        assertThat(AsyncHelloService.COUNTER.get()).isEqualTo(1);
    }

    @Test
    public void testAsyncRetryMethodThrows(AsyncHelloService helloService) {
        AsyncHelloService.COUNTER.set(0);
        assertThatThrownBy(() -> {
            helloService.retry(AsyncHelloService.Result.FAILURE).get();
        }).isExactlyInstanceOf(ExecutionException.class).hasCauseExactlyInstanceOf(IOException.class);
        assertThat(AsyncHelloService.COUNTER.get()).isEqualTo(3);
    }

    @Test
    public void testAsyncRetryFutureCompletesExceptionally(AsyncHelloService helloService) {
        AsyncHelloService.COUNTER.set(0);
        assertThatThrownBy(() -> {
            helloService.retry(AsyncHelloService.Result.COMPLETE_EXCEPTIONALLY).get();
        }).isExactlyInstanceOf(ExecutionException.class).hasCauseExactlyInstanceOf(IOException.class);
        assertThat(AsyncHelloService.COUNTER.get()).isEqualTo(1);
    }

    @Test
    public void testAsyncRetryFallbackMethodThrows(AsyncHelloService helloService)
            throws IOException, InterruptedException, ExecutionException {
        AsyncHelloService.COUNTER.set(0);
        assertThat(helloService.retryWithFallback(AsyncHelloService.Result.FAILURE).get()).isEqualTo("Fallback");
        assertThat(AsyncHelloService.COUNTER.get()).isEqualTo(3);
    }

    @Test
    public void testAsyncRetryFallbackFutureCompletesExceptionally(AsyncHelloService helloService) {
        AsyncHelloService.COUNTER.set(0);
        assertThatThrownBy(() -> {
            helloService.retryWithFallback(AsyncHelloService.Result.COMPLETE_EXCEPTIONALLY).get();
        }).isExactlyInstanceOf(ExecutionException.class).hasCauseExactlyInstanceOf(IOException.class);
        assertThat(AsyncHelloService.COUNTER.get()).isEqualTo(1);
    }
}

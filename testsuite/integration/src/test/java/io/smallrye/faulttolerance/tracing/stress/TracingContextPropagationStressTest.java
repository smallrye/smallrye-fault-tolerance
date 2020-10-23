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
package io.smallrye.faulttolerance.tracing.stress;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import io.opentracing.Scope;
import io.opentracing.mock.MockSpan;
import io.opentracing.util.GlobalTracerTestUtil;
import io.smallrye.faulttolerance.util.FaultToleranceIntegrationTest;

@FaultToleranceIntegrationTest
public class TracingContextPropagationStressTest {
    @AfterAll
    public static void reset() {
        GlobalTracerTestUtil.resetGlobalTracer();
    }

    @Test
    public void test(Service service) throws ExecutionException, InterruptedException {
        // 100 iterations so that the test doesn't run way too long
        for (int i = 0; i < 100; i++) {
            Service.tracer.reset();
            doTest(service);
        }
    }

    private void doTest(Service service) throws ExecutionException, InterruptedException {
        try (Scope ignored = Service.tracer.buildSpan("parent").startActive(true)) {
            assertThat(service.hello().toCompletableFuture().get()).isEqualTo("fallback");
        }

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            assertThat(Service.tracer.finishedSpans()).hasSize(5);
        });

        List<MockSpan> spans = Service.tracer.finishedSpans();
        for (MockSpan mockSpan : spans) {
            assertThat(mockSpan.context().traceId()).isEqualTo(spans.get(0).context().traceId());
        }

        // if timeout occurs, subsequent retries/fallback can be interleaved with the execution that timed out,
        // resulting in varying span order
        assertThat(countSpansWithOperationName(spans, "hello")).isEqualTo(3);
        assertThat(countSpansWithOperationName(spans, "fallback")).isEqualTo(1);
        assertThat(countSpansWithOperationName(spans, "parent")).isEqualTo(1);
    }

    private long countSpansWithOperationName(List<MockSpan> spans, String operationName) {
        return spans.stream().filter(span -> span.operationName().equals(operationName)).count();
    }
}

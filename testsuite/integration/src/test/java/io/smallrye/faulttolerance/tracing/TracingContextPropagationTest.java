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
package io.smallrye.faulttolerance.tracing;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.mock.MockSpan;
import io.opentracing.util.GlobalTracerTestUtil;
import io.smallrye.faulttolerance.util.FaultToleranceIntegrationTest;

@FaultToleranceIntegrationTest
public class TracingContextPropagationTest {
    @BeforeEach
    public void cleanUp() {
        Service.mockTracer.reset();
    }

    @AfterAll
    public static void reset() {
        GlobalTracerTestUtil.resetGlobalTracer();
    }

    @Test
    public void testCircuitBreakerOpens(Service service) {
        Span span = Service.mockTracer.buildSpan("parent").start();
        try (Scope ignored = Service.mockTracer.scopeManager().activate(span)) {
            assertThat(service.foo()).isEqualTo("fallback");
        } finally {
            span.finish();
        }

        List<MockSpan> mockSpans = Service.mockTracer.finishedSpans();
        assertThat(mockSpans).hasSize(4);
        //test spans are part of the same trace
        for (MockSpan mockSpan : mockSpans) {
            assertThat(mockSpan.context().traceId()).isEqualTo(mockSpans.get(0).context().traceId());
        }
        assertThat(mockSpans.get(0).operationName()).isEqualTo("foo");
        assertThat(mockSpans.get(1).operationName()).isEqualTo("foo");
        assertThat(mockSpans.get(2).operationName()).isEqualTo("foo");
        assertThat(mockSpans.get(3).operationName()).isEqualTo("parent");
    }

    @Test
    public void testAsyncCircuitBreakerOpens(Service service) throws ExecutionException, InterruptedException {
        Span span = Service.mockTracer.buildSpan("parent").start();
        try (Scope ignored = Service.mockTracer.scopeManager().activate(span)) {
            assertThat(service.asyncFoo().toCompletableFuture().get()).isEqualTo("asyncFallback");
        } finally {
            span.finish();
        }

        List<MockSpan> mockSpans = Service.mockTracer.finishedSpans();
        assertThat(mockSpans).hasSize(4);
        for (MockSpan mockSpan : mockSpans) {
            assertThat(mockSpan.context().traceId()).isEqualTo(mockSpans.get(0).context().traceId());
        }
        assertThat(mockSpans.get(0).operationName()).isEqualTo("asyncFoo");
        assertThat(mockSpans.get(1).operationName()).isEqualTo("asyncFoo");
        assertThat(mockSpans.get(2).operationName()).isEqualTo("asyncFoo");
        assertThat(mockSpans.get(3).operationName()).isEqualTo("parent");
    }
}

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

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.concurrent.ExecutionException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.opentracing.Scope;
import io.opentracing.mock.MockSpan;
import io.smallrye.faulttolerance.TestArchive;

/**
 * @author Pavol Loffay
 */
@RunWith(Arquillian.class)
public class TracingContextPropagationTest {

    @Deployment
    public static JavaArchive createTestArchive() {
        return TestArchive.createBase(TracingContextPropagationTest.class)
                .addPackage(TracingContextPropagationTest.class.getPackage());
    }

    @Before
    public void cleanUp() {
        Service.mockTracer.reset();
    }

    @Test
    public void testCircuitBreakerOpens(Service service) {
        try (Scope ignored = Service.mockTracer.buildSpan("parent").startActive(true)) {
            assertEquals("fallback", service.foo());
        }

        List<MockSpan> mockSpans = Service.mockTracer.finishedSpans();
        assertEquals(4, mockSpans.size());
        //test spans are part of the same trace
        for (MockSpan mockSpan : mockSpans) {
            assertEquals(mockSpans.get(0).context().traceId(), mockSpan.context().traceId());
        }
        assertEquals("foo", mockSpans.get(0).operationName());
        assertEquals("foo", mockSpans.get(1).operationName());
        assertEquals("foo", mockSpans.get(2).operationName());
        assertEquals("parent", mockSpans.get(3).operationName());
    }

    @Test
    public void testAsyncCircuitBreakerOpens(Service service) throws ExecutionException, InterruptedException {
        try (Scope ignored = Service.mockTracer.buildSpan("parent").startActive(true)) {
            assertEquals("asyncFallback", service.asyncFoo().toCompletableFuture().get());
        }

        List<MockSpan> mockSpans = Service.mockTracer.finishedSpans();
        assertEquals(4, mockSpans.size());
        for (MockSpan mockSpan : mockSpans) {
            assertEquals(mockSpans.get(0).context().traceId(), mockSpan.context().traceId());
        }
        assertEquals("asyncFoo", mockSpans.get(0).operationName());
        assertEquals("asyncFoo", mockSpans.get(1).operationName());
        assertEquals("asyncFoo", mockSpans.get(2).operationName());
        assertEquals("parent", mockSpans.get(3).operationName());
    }
}

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
package io.smallrye.faulttolerance.async.compstage.retry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.smallrye.faulttolerance.TestArchive;

@RunWith(Arquillian.class)
public class AsynchronousCompletionStageRetryTest {
    @Deployment
    public static JavaArchive createTestArchive() {
        return TestArchive.createBase(AsynchronousCompletionStageRetryTest.class)
                .addPackage(AsynchronousCompletionStageRetryTest.class.getPackage());
    }

    @Test
    public void testAsyncRetrySuccess(AsyncHelloService helloService)
            throws IOException, InterruptedException, ExecutionException {
        AsyncHelloService.COUNTER.set(0);
        assertEquals("Hello", helloService.retry(AsyncHelloService.Result.SUCCESS).toCompletableFuture().get());
        assertEquals(1, AsyncHelloService.COUNTER.get());
    }

    @Test
    public void testAsyncRetryMethodThrows(AsyncHelloService helloService) throws IOException, InterruptedException {
        AsyncHelloService.COUNTER.set(0);
        try {
            CompletableFuture<String> result = helloService.retry(AsyncHelloService.Result.FAILURE).toCompletableFuture();
            result.get();
            fail();
        } catch (ExecutionException expected) {
            assertTrue(expected.getCause() instanceof IOException);
        }
        assertEquals(3, AsyncHelloService.COUNTER.get());
    }

    @Test
    public void testAsyncRetryFutureCompletesExceptionally(AsyncHelloService helloService)
            throws IOException, InterruptedException {
        AsyncHelloService.COUNTER.set(0);
        try {
            helloService.retry(AsyncHelloService.Result.COMPLETE_EXCEPTIONALLY).toCompletableFuture().get();
            fail();
        } catch (ExecutionException expected) {
            assertTrue(expected.getCause() instanceof IOException);
        }
        assertEquals(3, AsyncHelloService.COUNTER.get());
    }

    @Test
    public void testAsyncRetryFallbackMethodThrows(AsyncHelloService helloService)
            throws IOException, InterruptedException, ExecutionException {
        AsyncHelloService.COUNTER.set(0);
        assertEquals("Fallback", helloService.retryWithFallback(AsyncHelloService.Result.FAILURE).toCompletableFuture().get());
        assertEquals(3, AsyncHelloService.COUNTER.get());
    }

    @Test
    public void testAsyncRetryFallbackFutureCompletesExceptionally(AsyncHelloService helloService)
            throws IOException, InterruptedException, ExecutionException {
        AsyncHelloService.COUNTER.set(0);
        assertEquals("Fallback",
                helloService.retryWithFallback(AsyncHelloService.Result.COMPLETE_EXCEPTIONALLY).toCompletableFuture().get());
        assertEquals(3, AsyncHelloService.COUNTER.get());
    }
}

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
package io.smallrye.faulttolerance.async.fallback;

import static java.util.concurrent.CompletableFuture.completedFuture;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import javax.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.faulttolerance.Asynchronous;
import org.eclipse.microprofile.faulttolerance.Fallback;

@ApplicationScoped
public class AsyncHelloService {
    @Asynchronous
    @Fallback(fallbackMethod = "fallback")
    public Future<String> hello(Result result) throws IOException {
        switch (result) {
            case FAILURE:
                throw new IOException("Simulated IO error");
            case COMPLETE_EXCEPTIONALLY:
                CompletableFuture<String> future = new CompletableFuture<>();
                future.completeExceptionally(new IOException("Simulated IO error"));
                return future;
            default:
                return completedFuture("Hello");
        }
    }

    public Future<String> fallback(Result result) {
        return completedFuture("Fallback");
    }

    enum Result {
        SUCCESS,
        FAILURE,
        COMPLETE_EXCEPTIONALLY
    }
}

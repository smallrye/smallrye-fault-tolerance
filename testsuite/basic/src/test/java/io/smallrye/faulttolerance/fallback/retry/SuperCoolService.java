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
package io.smallrye.faulttolerance.fallback.retry;

import java.util.concurrent.atomic.AtomicInteger;

import jakarta.enterprise.context.ApplicationScoped;

import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;

@ApplicationScoped
public class SuperCoolService {

    private AtomicInteger counter = new AtomicInteger(0);

    @Retry(maxRetries = 2)
    @Fallback(fallbackMethod = "getFallback")
    @CircuitBreaker(requestVolumeThreshold = 5)
    public String ping() {
        counter.incrementAndGet();
        throw new IllegalStateException();
    }

    String getFallback() {
        return SuperCoolService.class.getName();
    }

    AtomicInteger getCounter() {
        return counter;
    }

}

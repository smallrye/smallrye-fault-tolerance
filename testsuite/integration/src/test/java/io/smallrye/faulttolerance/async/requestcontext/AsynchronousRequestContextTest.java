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
package io.smallrye.faulttolerance.async.requestcontext;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.ExecutionException;

import javax.enterprise.context.control.RequestContextController;
import javax.inject.Inject;

import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.util.FaultToleranceIntegrationTest;

@FaultToleranceIntegrationTest
public class AsynchronousRequestContextTest {
    @Inject
    RequestContextController rcc;

    @Test
    public void testRequestContextActive(AsyncService asyncService) throws InterruptedException, ExecutionException {
        // Weld JUnit has a dumb context implementation that doesn't support context propagation,
        // need to use the real request context
        boolean activated = rcc.activate();
        try {
            assertThat(asyncService.perform().get()).isEqualTo("ok");
        } finally {
            if (activated) {
                rcc.deactivate();
            }
        }
    }
}

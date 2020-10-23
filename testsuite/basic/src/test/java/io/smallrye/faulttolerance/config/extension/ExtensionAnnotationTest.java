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
package io.smallrye.faulttolerance.config.extension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.temporal.ChronoUnit;

import javax.inject.Inject;

import org.jboss.weld.junit5.auto.AddExtensions;
import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.FaultToleranceOperations;
import io.smallrye.faulttolerance.config.FaultToleranceOperation;
import io.smallrye.faulttolerance.config.RetryConfig;
import io.smallrye.faulttolerance.util.FaultToleranceBasicTest;

@FaultToleranceBasicTest
@AddExtensions(CustomExtension.class)
public class ExtensionAnnotationTest {
    @Inject
    FaultToleranceOperations ops;

    @Inject
    UnconfiguredService service;

    @Test
    public void testAnnotationAddedByExtension() throws NoSuchMethodException, SecurityException {
        FaultToleranceOperation ping = ops.get(UnconfiguredService.class, UnconfiguredService.class.getMethod("ping"));
        assertThat(ping).isNotNull();
        assertThat(ping.hasRetry()).isTrue();

        RetryConfig fooRetry = ping.getRetry();
        // Method-level
        assertThat(fooRetry.get(RetryConfig.MAX_RETRIES, Integer.class)).isEqualTo(2);
        // Default value
        assertThat(fooRetry.get(RetryConfig.DELAY_UNIT, ChronoUnit.class)).isEqualTo(ChronoUnit.MILLIS);

        UnconfiguredService.COUNTER.set(0);
        assertThatThrownBy(() -> {
            service.ping();
        }).isExactlyInstanceOf(IllegalStateException.class);
        assertThat(UnconfiguredService.COUNTER.get()).isEqualTo(3);
    }
}

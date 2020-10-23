/*
 * Copyright 2020 Red Hat, Inc, and individual contributors.
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
package io.smallrye.faulttolerance.fallback.retry.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.Tag;
import org.eclipse.microprofile.metrics.annotation.RegistryType;
import org.junit.jupiter.api.Test;

import io.smallrye.faulttolerance.util.FaultToleranceBasicTest;

@FaultToleranceBasicTest
public class FallbackRetryMetricsTest {
    @Test
    public void test(MyService service, @RegistryType(type = MetricRegistry.Type.BASE) MetricRegistry metrics) {
        String result = service.hello();
        assertThat(result).isEqualTo("fallback");

        assertThat(metrics.counter("ft.invocations.total",
                new Tag("method", "io.smallrye.faulttolerance.fallback.retry.metrics.MyService.hello"),
                new Tag("result", "valueReturned"),
                new Tag("fallback", "applied"))
                .getCount()).isEqualTo(1);

        assertThat(metrics.counter("ft.retry.retries.total",
                new Tag("method", "io.smallrye.faulttolerance.fallback.retry.metrics.MyService.hello"))
                .getCount()).isEqualTo(3);
        assertThat(metrics.counter("ft.retry.calls.total",
                new Tag("method", "io.smallrye.faulttolerance.fallback.retry.metrics.MyService.hello"),
                new Tag("retried", "true"),
                new Tag("retryResult", "maxRetriesReached"))
                .getCount()).isEqualTo(1);
    }
}

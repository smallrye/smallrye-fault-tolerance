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

import static io.opentelemetry.api.common.AttributeKey.stringKey;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.metrics.data.LongPointData;
import io.smallrye.faulttolerance.minimptel.MetricsAccess;
import io.smallrye.faulttolerance.util.FaultToleranceBasicTest;

@FaultToleranceBasicTest
public class FallbackRetryMetricsTest {
    @Test
    public void test(MyService service, MetricsAccess metrics) {
        String result = service.hello();
        assertThat(result).isEqualTo("fallback");

        assertThat(metrics.get(LongPointData.class, "ft.invocations.total", Attributes.of(
                stringKey("method"), "io.smallrye.faulttolerance.fallback.retry.metrics.MyService.hello",
                stringKey("result"), "valueReturned",
                stringKey("fallback"), "applied"))
                .getValue()).isEqualTo(1);

        assertThat(metrics.get(LongPointData.class, "ft.retry.retries.total", Attributes.of(
                stringKey("method"), "io.smallrye.faulttolerance.fallback.retry.metrics.MyService.hello"))
                .getValue()).isEqualTo(3);
        assertThat(metrics.get(LongPointData.class, "ft.retry.calls.total", Attributes.of(
                stringKey("method"), "io.smallrye.faulttolerance.fallback.retry.metrics.MyService.hello",
                stringKey("retried"), "true",
                stringKey("retryResult"), "maxRetriesReached"))
                .getValue()).isEqualTo(1);
    }
}

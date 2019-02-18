/*
 * Copyright 2019 Red Hat, Inc, and individual contributors.
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
package io.smallrye.faulttolerance.metrics;

import io.smallrye.faulttolerance.CommandListener;
import io.smallrye.faulttolerance.config.FaultToleranceOperation;
import org.eclipse.microprofile.metrics.Histogram;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.MetricType;
import org.jboss.logging.Logger;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 * <br>
 * Date: 2/18/19
 */
public class BulkheadWaitRecorder implements CommandListener {

    private static final Logger LOGGER = Logger.getLogger(BulkheadWaitRecorder.class);

    private final long enqueuedTime;
    private final MetricRegistry registry;

    public BulkheadWaitRecorder(MetricRegistry registry) {
        this.registry = registry;
        enqueuedTime = System.nanoTime();
    }

    @Override
    public void beforeExecution(FaultToleranceOperation operation) {
        try {
            histogramOf(operation)
                    .update(System.nanoTime() - enqueuedTime);
        } catch (Exception any) {
            LOGGER.warn("Failed to update metrics", any);
        }
    }

    private Histogram histogramOf(FaultToleranceOperation operation) {
        String name = MetricNames.metricsPrefix(operation.getMethod()) + MetricNames.BULKHEAD_WAITING_DURATION;
        Histogram histogram = registry.getHistograms().get(name);
        if (histogram == null) {
            synchronized (operation) {
                histogram = registry.getHistograms().get(name);
                if (histogram == null) {
                    histogram = registry.histogram(MetricsCollectorFactory.metadataOf(name, MetricType.HISTOGRAM));
                }
            }
        }
        return histogram;
    }
}

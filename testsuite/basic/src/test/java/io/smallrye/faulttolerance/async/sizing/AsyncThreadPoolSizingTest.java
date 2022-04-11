package io.smallrye.faulttolerance.async.sizing;

import io.smallrye.faulttolerance.util.FaultToleranceBasicTest;
import io.smallrye.faulttolerance.util.WithSystemProperty;

@WithSystemProperty(key = "io.smallrye.faulttolerance.mainThreadPoolSize", value = "10")
@WithSystemProperty(key = "io.smallrye.faulttolerance.mainThreadPoolQueueSize", value = "0")
@FaultToleranceBasicTest
public class AsyncThreadPoolSizingTest extends AbstractAsyncThreadPoolSizingTest {
}

package io.smallrye.faulttolerance.async.sizing;

import io.smallrye.faulttolerance.util.FaultToleranceBasicTest;
import io.smallrye.faulttolerance.util.WithSystemProperty;

@WithSystemProperty(key = "io.smallrye.faulttolerance.globalThreadPoolSize", value = "10")
@WithSystemProperty(key = "io.smallrye.faulttolerance.mainThreadPoolQueueSize", value = "0")
@FaultToleranceBasicTest
public class AsyncThreadPoolSizingOldConfigTest extends AbstractAsyncThreadPoolSizingTest {
}

package io.smallrye.faulttolerance.async.sizing;

import org.junitpioneer.jupiter.SetSystemProperty;

import io.smallrye.faulttolerance.util.FaultToleranceBasicTest;

@SetSystemProperty(key = "io.smallrye.faulttolerance.globalThreadPoolSize", value = "10")
@SetSystemProperty(key = "io.smallrye.faulttolerance.mainThreadPoolQueueSize", value = "0")
@FaultToleranceBasicTest
public class AsyncThreadPoolSizingOldConfigTest extends AbstractAsyncThreadPoolSizingTest {
}

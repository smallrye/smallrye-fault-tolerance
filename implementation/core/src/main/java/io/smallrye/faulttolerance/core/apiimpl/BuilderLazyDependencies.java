package io.smallrye.faulttolerance.core.apiimpl;

import java.util.concurrent.ExecutorService;

import io.smallrye.faulttolerance.core.event.loop.EventLoop;
import io.smallrye.faulttolerance.core.timer.Timer;

// dependencies that must NOT be accessed eagerly; these are NOT safe to use during static initialization
public interface BuilderLazyDependencies {
    boolean ftEnabled();

    ExecutorService asyncExecutor();

    EventLoop eventLoop();

    Timer timer();
}

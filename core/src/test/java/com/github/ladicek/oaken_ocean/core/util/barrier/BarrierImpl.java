package com.github.ladicek.oaken_ocean.core.util.barrier;

import java.util.concurrent.locks.AbstractQueuedSynchronizer;

/**
 * Implementation adapted from {@link AbstractQueuedSynchronizer}.
 */
final class BarrierImpl implements Barrier {
    private static class Sync extends AbstractQueuedSynchronizer {
        boolean isSignalled() {
            return getState() != 0;
        }

        protected int tryAcquireShared(int ignore) {
            return isSignalled() ? 1 : -1;
        }

        protected boolean tryReleaseShared(int ignore) {
            setState(1);
            return true;
        }
    }

    private final Sync sync = new Sync();
    private final boolean interruptible;

    BarrierImpl(boolean interruptible) {
        this.interruptible = interruptible;
    }

    @Override
    public void await() throws InterruptedException {
        if (interruptible) {
            sync.acquireSharedInterruptibly(1);
        } else {
            sync.acquireShared(1);
        }
    }

    @Override
    public void open() {
        sync.releaseShared(1);
    }
}

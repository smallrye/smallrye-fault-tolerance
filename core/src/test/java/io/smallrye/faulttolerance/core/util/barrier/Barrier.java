package io.smallrye.faulttolerance.core.util.barrier;

public interface Barrier {
    void await() throws InterruptedException;

    void open();

    static Barrier interruptible() {
        return new BarrierImpl(true);
    }

    static Barrier noninterruptible() {
        return new BarrierImpl(false);
    }
}

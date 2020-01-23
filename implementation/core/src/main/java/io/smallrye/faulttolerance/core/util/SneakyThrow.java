package io.smallrye.faulttolerance.core.util;

// TODO
public class SneakyThrow {
    private SneakyThrow() {
    }

    @SuppressWarnings("unchecked")
    public static <E extends Throwable> Exception sneakyThrow(Throwable e) throws E {
        throw (E) e;
    }
}

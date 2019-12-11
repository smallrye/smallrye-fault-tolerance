package io.smallrye.faulttolerance.core.util;

public class SneakyThrow {
    private SneakyThrow() {
    }

    @SuppressWarnings("unchecked")
    public static <E extends Throwable> void sneakyThrow(Throwable e) throws E {
        throw (E) e;
    }
}

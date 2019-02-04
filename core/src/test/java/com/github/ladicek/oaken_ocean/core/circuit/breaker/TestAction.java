package com.github.ladicek.oaken_ocean.core.circuit.breaker;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;

public class TestAction<V> implements Callable<V> {
    private final List<Callable<V>> actions;

    @SafeVarargs
    public static <V> TestAction<V> create(Callable<V>... actions) {
        return new TestAction<>(Arrays.asList(actions));
    }

    private TestAction(List<Callable<V>> actions) {
        this.actions = new ArrayList<>(actions);
    }

    @Override
    public V call() throws Exception {
        if (actions.isEmpty()) {
            throw new AssertionError("No more actions to perform");
        }

        Callable<V> action = actions.remove(0);
        return action.call();
    }
}

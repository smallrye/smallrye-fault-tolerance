package io.smallrye.faulttolerance.core.util;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Contains a sequence of {@link Runnable} actions and makes sure that they are only executed once.
 */
public final class Initializer {
    private final Runnable[] actions;
    private final AtomicBoolean ran = new AtomicBoolean(false);

    public Initializer(Runnable action) {
        this(Collections.singletonList(action));
    }

    public Initializer(List<Runnable> actions) {
        Preconditions.checkNotNull(actions, "List of actions must be set");
        this.actions = actions.toArray(new Runnable[0]);
    }

    public void runOnce() {
        if (ran.compareAndSet(false, true)) {
            for (Runnable action : actions) {
                action.run();
            }
        }
    }
}

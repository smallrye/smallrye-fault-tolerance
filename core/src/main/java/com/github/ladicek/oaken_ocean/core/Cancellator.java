package com.github.ladicek.oaken_ocean.core;

import java.util.ArrayList;
import java.util.List;

/*
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 */
public class Cancellator {
    private boolean canceled = false;
    private boolean mayInterruptIfRunning = false;
    private final List<CancelAction> cancelActions = new ArrayList<>();

    public synchronized void addCancelAction(CancelAction action) {
        if (canceled) {
            action.cancel(mayInterruptIfRunning);
        } else {
            cancelActions.add(action);
        }
    }

    public synchronized void cancel(boolean mayInterruptIfRunning) {
        this.mayInterruptIfRunning = mayInterruptIfRunning;
        canceled = true;
        cancelActions.forEach(action -> action.cancel(mayInterruptIfRunning));
    }

    @FunctionalInterface
    public interface CancelAction {
        void cancel(boolean mayInterruptIfRunning);
    }
}

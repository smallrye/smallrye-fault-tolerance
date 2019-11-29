package com.github.ladicek.oaken_ocean.core;

import java.util.ArrayList;
import java.util.List;
/*
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 */
// mstodo remove or use
public class Cancelator {
    private boolean canceled = false;
    private final List<Runnable> cancelActions = new ArrayList<>();

    public synchronized void addCancelAction(Runnable runnable) {
        if (canceled) {
            runnable.run();
        } else {
            cancelActions.add(runnable);
        }
    }

    public synchronized void cancel() {
        canceled = true;
        cancelActions.forEach(Runnable::run);
    }
}

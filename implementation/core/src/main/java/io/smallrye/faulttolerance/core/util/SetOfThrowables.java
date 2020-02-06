package io.smallrye.faulttolerance.core.util;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class SetOfThrowables {
    public static final SetOfThrowables EMPTY = new SetOfThrowables(Collections.emptySet());
    public static final SetOfThrowables ALL = new SetOfThrowables(Collections.singleton(Throwable.class));

    /**
     * @param classes classes to include
     * @return a set of throwables without any additional constraints.
     */
    public static SetOfThrowables create(List<Class<? extends Throwable>> classes) {
        Set<Class<? extends Throwable>> set = Collections.newSetFromMap(new ConcurrentHashMap<>());
        set.addAll(classes);
        return new SetOfThrowables(set);
    }

    private final Set<Class<? extends Throwable>> classes;

    private SetOfThrowables(Set<Class<? extends Throwable>> classes) {
        this.classes = classes;
    }

    /**
     * @param searchedFor a class to check
     * @return whether {@code searchedFor} is a subtype of (at least) one of the types in this set.
     *         Note that subtyping is a reflexive relation, so a type is always a subtype of itself.
     */
    public boolean includes(Class<? extends Throwable> searchedFor) {
        for (Class<? extends Throwable> clazz : classes) {
            if (clazz.isAssignableFrom(searchedFor)) {
                return true;
            }
        }

        return false;
    }
}

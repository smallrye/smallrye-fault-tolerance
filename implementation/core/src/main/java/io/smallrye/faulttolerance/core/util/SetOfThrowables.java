package io.smallrye.faulttolerance.core.util;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class SetOfThrowables {
    public static final SetOfThrowables EMPTY = new SetOfThrowables(Collections.emptySet());
    public static final SetOfThrowables ALL = new SetOfThrowables(Collections.singleton(Throwable.class));

    /**
     * Creates a set consisting of a single throwable class. The set can later be inspected using {@link #includes(Class)}.
     *
     * @param clazz a single throwable class to include in the set
     * @return a singleton set of throwable classes
     */
    public static SetOfThrowables create(Class<? extends Throwable> clazz) {
        return create(Collections.singletonList(clazz));
    }

    /**
     * Creates a set of throwable classes that can later be inspected using {@link #includes(Class)}.
     *
     * @param classes throwable classes to include in the set
     * @return a set of throwable classes
     */
    public static SetOfThrowables create(Collection<Class<? extends Throwable>> classes) {
        Set<Class<? extends Throwable>> set = new HashSet<>(classes);
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

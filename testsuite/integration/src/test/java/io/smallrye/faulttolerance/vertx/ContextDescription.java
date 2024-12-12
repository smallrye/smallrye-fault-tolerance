package io.smallrye.faulttolerance.vertx;

import java.util.Locale;
import java.util.Objects;

public final class ContextDescription {
    public final ExecutionStyle executionStyle;
    public final String contextClass;
    public final String uuid;
    public final String contextHash;

    ContextDescription(ExecutionStyle executionStyle, String contextClass, String uuid, String contextHash) {
        this.executionStyle = executionStyle;
        this.contextClass = contextClass;
        this.contextHash = contextHash;
        this.uuid = uuid;
    }

    public boolean isDuplicatedContext() {
        return "DuplicatedContext".equals(contextClass);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ContextDescription)) {
            return false;
        }
        ContextDescription that = (ContextDescription) o;
        return Objects.equals(executionStyle, that.executionStyle)
                && Objects.equals(contextClass, that.contextClass)
                && Objects.equals(uuid, that.uuid)
                && Objects.equals(contextHash, that.contextHash);
    }

    @Override
    public int hashCode() {
        return Objects.hash(executionStyle, contextClass, uuid, contextHash);
    }

    @Override
    public String toString() {
        return executionStyle.toString().toLowerCase(Locale.ROOT)
                + "|" + contextClass
                + "|" + uuid
                + "|" + contextHash;
    }
}

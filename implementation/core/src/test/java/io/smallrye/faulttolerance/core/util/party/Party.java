package io.smallrye.faulttolerance.core.util.party;

public interface Party {
    Participant participant();

    Organizer organizer();

    static Party create(int participants) {
        return new PartyImpl(participants);
    }

    interface Participant {
        void attend() throws InterruptedException;
    }

    interface Organizer {
        void waitForAll() throws InterruptedException;

        void disband();
    }
}

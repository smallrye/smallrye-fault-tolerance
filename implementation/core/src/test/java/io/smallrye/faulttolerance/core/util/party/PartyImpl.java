package io.smallrye.faulttolerance.core.util.party;

import java.util.concurrent.CountDownLatch;

final class PartyImpl implements Party {
    private final Participant participant;

    private final Organizer organizer;

    PartyImpl(int participants) {
        CountDownLatch arriveLatch = new CountDownLatch(participants);
        CountDownLatch disbandLatch = new CountDownLatch(1);

        participant = new ParticipantImpl(arriveLatch, disbandLatch);
        organizer = new OrganizerImpl(arriveLatch, disbandLatch);
    }

    @Override
    public Participant participant() {
        return participant;
    }

    @Override
    public Organizer organizer() {
        return organizer;
    }

    private static final class ParticipantImpl implements Participant {
        private final CountDownLatch arriveLatch;

        private final CountDownLatch disbandLatch;

        ParticipantImpl(CountDownLatch arriveLatch, CountDownLatch disbandLatch) {
            this.arriveLatch = arriveLatch;
            this.disbandLatch = disbandLatch;
        }

        @Override
        public void attend() throws InterruptedException {
            arriveLatch.countDown();
            disbandLatch.await();
        }
    }

    private static final class OrganizerImpl implements Organizer {
        private final CountDownLatch arriveLatch;

        private final CountDownLatch disbandLatch;

        OrganizerImpl(CountDownLatch arriveLatch, CountDownLatch disbandLatch) {
            this.arriveLatch = arriveLatch;
            this.disbandLatch = disbandLatch;
        }

        @Override
        public void waitForAll() throws InterruptedException {
            arriveLatch.await();
        }

        @Override
        public void disband() {
            disbandLatch.countDown();
        }
    }
}

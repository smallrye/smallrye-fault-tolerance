/*
 * Copyright 2017 Red Hat, Inc, and individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.smallrye.faulttolerance.command.listener;

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.smallrye.faulttolerance.CommandListener;
import io.smallrye.faulttolerance.TestArchive;
import io.smallrye.faulttolerance.config.FaultToleranceOperation;

/**
 *
 * @author Martin Kouba
 */
@RunWith(Arquillian.class)
public class CommandListenerTest {

    static final List<String> ACTIONS = new CopyOnWriteArrayList<>();

    @Deployment
    public static JavaArchive createTestArchive() {
        return TestArchive.createBase(CommandListenerTest.class).addPackage(CommandListenerTest.class.getPackage());
    }

    @Test
    public void testListenerInvoked(HelloService helloService) {
        ACTIONS.clear();
        BravoListener.DESTROYED.set(0);

        assertEquals("2", helloService.hello());
        // Note that the first commands fails
        assertEquals(8, ACTIONS.size());
        assertEquals("bravo:before", ACTIONS.get(0));
        assertEquals("alpha:before", ACTIONS.get(1));
        assertEquals("bravo:after", ACTIONS.get(2));
        assertEquals("alpha:after", ACTIONS.get(3));
        assertEquals("bravo:before", ACTIONS.get(4));
        assertEquals("alpha:before", ACTIONS.get(5));
        assertEquals("bravo:after", ACTIONS.get(6));
        assertEquals("alpha:after", ACTIONS.get(7));

        // Verify the dependent listeners are destroyed correctly
        assertEquals(2, BravoListener.DESTROYED.get());
    }

    @ApplicationScoped
    static class AlphaListener implements CommandListener {

        @Override
        public void afterExecution(FaultToleranceOperation operation) {
            ACTIONS.add("alpha:after");
        }

        @Override
        public void beforeExecution(FaultToleranceOperation operation) {
            ACTIONS.add("alpha:before");
        }

    }

    @Dependent
    static class BravoListener implements CommandListener {

        static final AtomicInteger DESTROYED = new AtomicInteger(0);

        @Override
        public void afterExecution(FaultToleranceOperation operation) {
            ACTIONS.add("bravo:after");
        }

        @Override
        public void beforeExecution(FaultToleranceOperation operation) {
            ACTIONS.add("bravo:before");
        }

        @PreDestroy
        void destroyed() {
            DESTROYED.incrementAndGet();
        }

        @Override
        public int getPriority() {
            return 1;
        }

    }

}

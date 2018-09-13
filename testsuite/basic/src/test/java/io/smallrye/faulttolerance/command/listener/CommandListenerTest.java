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

import javax.enterprise.context.ApplicationScoped;

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

    @Deployment
    public static JavaArchive createTestArchive() {
        return TestArchive.createBase(CommandListenerTest.class).addPackage(CommandListenerTest.class.getPackage());
    }

    @Test
    public void testListenerInvoked(HelloService helloService) {
        CustomCommandListener.ACTIONS.clear();
        assertEquals("2", helloService.hello());
        // Note that first command fails
        assertEquals(4, CustomCommandListener.ACTIONS.size());
        assertEquals("before", CustomCommandListener.ACTIONS.get(0));
        assertEquals("after", CustomCommandListener.ACTIONS.get(1));
        assertEquals("before", CustomCommandListener.ACTIONS.get(2));
        assertEquals("after", CustomCommandListener.ACTIONS.get(3));
    }

    @ApplicationScoped
    static class CustomCommandListener implements CommandListener {

        static final List<String> ACTIONS = new CopyOnWriteArrayList<>();

        @Override
        public void afterExecution(FaultToleranceOperation operation) {
            ACTIONS.add("after");
        }

        @Override
        public void beforeExecution(FaultToleranceOperation operation) {
            ACTIONS.add("before");
        }

    }

}

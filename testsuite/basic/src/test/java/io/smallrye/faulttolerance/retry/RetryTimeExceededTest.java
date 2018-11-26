/*
 * Copyright 2018 Red Hat, Inc, and individual contributors.
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
package io.smallrye.faulttolerance.retry;

import io.smallrye.faulttolerance.TestArchive;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.inject.Inject;

import static org.junit.Assert.assertEquals;

/**
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 * <br>
 * Date: 11/23/18
 */
@RunWith(Arquillian.class)
public class RetryTimeExceededTest {

    @Deployment
    public static JavaArchive createTestArchive() {
        return TestArchive.createBase(io.smallrye.faulttolerance.retry.RetryTimeExceededTest.class)
                .addPackage(io.smallrye.faulttolerance.retry.RetryTimeExceededTest.class.getPackage());
    }

    @Inject
    private RetryTestBean retryTestBean;

    @After
    public void cleanUp() {
        retryTestBean.reset();
    }

    @Test
    public void shouldFallbackOnMaxDurationExceeded() {
        String result = retryTestBean.callWithMaxDuration500ms(600L);
        assertEquals("fallback", result);
    }
    @Test
    public void shouldNotFallbackIfTimeNotReached() {
        String result = retryTestBean.callWithMaxDuration2s(600L, 100L);
        assertEquals("call1", result);
    }
}

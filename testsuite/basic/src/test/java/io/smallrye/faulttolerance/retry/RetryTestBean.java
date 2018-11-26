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

import org.eclipse.microprofile.faulttolerance.Fallback;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;

import javax.enterprise.context.Dependent;

/**
 *
 * @author Michal Szynkiewicz, michal.l.szynkiewicz@gmail.com
 * <br>
 * Date: 11/23/18
 */
@Dependent
public class RetryTestBean {

    private int attempt = 0;

    @Retry(maxDuration = 500L, maxRetries = 2)
    @Fallback(fallbackMethod = "fallback")
    @Timeout(500L)
    public String callWithMaxDuration500ms(Long... sleepTime) {
        return call(sleepTime);
    }

    @Retry(maxDuration = 2000L, maxRetries = 2)
    @Fallback(fallbackMethod = "fallback")
    @Timeout(500L)
    public String callWithMaxDuration2s(Long... sleepTime) {
        return call(sleepTime);
    }

    private String call(Long[] sleepTime) {
        int attempt = this.attempt++;
        try {
            if (attempt < sleepTime.length) {
                Thread.sleep(sleepTime[attempt]);
            }
        } catch (InterruptedException ignored) {
        }
        return "call" + attempt;
    }

    public String fallback(Long... ignored) {
        return "fallback";
    }

    public void reset() {
        attempt = 0;
    }
}

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sling.ide.test.impl.helpers;

import static org.junit.Assert.assertThat;

import java.util.concurrent.Callable;

import org.hamcrest.CoreMatchers;
import org.hamcrest.Matcher;

import junit.framework.AssertionFailedError;

public class Poller {

    private static final long DEFAULT_POLL_WAIT_MILLIS = 5000;
    private static final long DEFAULT_DELAY_MILLIS = 100;
    
    private long timeoutMillis;
    
    public Poller() {
        this(DEFAULT_POLL_WAIT_MILLIS);
    }
    
    public Poller(long timeoutMillis) {
        this.timeoutMillis = timeoutMillis;
    }

    public void pollUntilSuccessful(Runnable r) throws InterruptedException {

        long cutoff = System.currentTimeMillis() + timeoutMillis;

        Throwable lastError = null;

        int numAttempt = 0;
        while (true) {
        	numAttempt++;
            try {
                r.run();
                break;
            } catch (RuntimeException| AssertionError e) {
                lastError = e;
                // skip
            }

            if (System.currentTimeMillis() >= cutoff) {
                String message = "Runnable " + r + " did not succeed in the allocated "
                        + timeoutMillis + " ms (" + numAttempt + " attempts)";
                if (lastError != null) {
                	throw new AssertionError(message, lastError);
                } else {
                	throw new AssertionFailedError(message);
                }
            }

            Thread.sleep(DEFAULT_DELAY_MILLIS);
        }
    }

    public <V> V pollUntil(final Callable<V> callable, final Matcher<V> matcher) throws InterruptedException {

        final Object[] holder = new Object[1];

        pollUntilSuccessful(new Runnable() {
            @Override
            public void run() {
                V result;
                try {
                    result = callable.call();
                    holder[0] = result;
                } catch (RuntimeException e) {
                    throw e;
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                assertThat(result, matcher);
            }

            @Override
            public String toString() {
                return callable.toString();
            }
        });

        // safe, since only we write in holder[0]
        return (V) holder[0];
    }

    public void pollUntilTrue(final Callable<Boolean> callable) throws InterruptedException {
        pollUntil(callable, CoreMatchers.equalTo(true));
    }
}

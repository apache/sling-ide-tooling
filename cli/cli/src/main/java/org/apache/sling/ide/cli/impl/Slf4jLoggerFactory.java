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
package org.apache.sling.ide.cli.impl;


import org.apache.sling.ide.log.Logger;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ServiceScope;
import org.slf4j.LoggerFactory;

@Component(service = Logger.class, scope = ServiceScope.BUNDLE)
public class Slf4jLoggerFactory implements Logger {

    private static final long PERF_IGNORE_THRESHOLD = 50;

    private final org.slf4j.Logger wrapped = LoggerFactory.getLogger(Slf4jLoggerFactory.class);

    private String marker;
    
    protected void activate(ComponentContext ctx) {
        marker = "[" + ctx.getUsingBundle().getSymbolicName() + "] ";
        wrapped.info(marker + "Logger initialized");
    }

    @Override
    public void warn(String message, Throwable cause) {
        wrapped.warn( marker + message, cause);
    }

    @Override
    public void warn(String message) {
        wrapped.warn(marker + message);
    }

    @Override
    public void trace(String message, Throwable error) {
        wrapped.info(marker + message, error);
    }

    @Override
    public void trace(String message, Object... arguments) {

        // this is probably a horribly slow implementation, but it does not matter
        for (int i = 0; i < arguments.length; i++) {
            message = message.replace("{" + i + "}", String.valueOf(arguments[i]));
        }

        wrapped.info(marker + message);
    }

    @Override
    public void error(String message, Throwable cause) {
        wrapped.error(marker + message, cause);
    }

    @Override
    public void error(String message) {
        wrapped.error(marker + message);
    }

    @Override
    public void tracePerformance(String message, long duration, Object... arguments) {
        if (duration < PERF_IGNORE_THRESHOLD) {
            return;
        }
        trace(message + " took " + duration + " ms", arguments);
    }

}

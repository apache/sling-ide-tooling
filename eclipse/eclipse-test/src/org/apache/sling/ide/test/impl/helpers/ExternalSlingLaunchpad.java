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

import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.junit.rules.ExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.stream.JsonReader;

import junit.framework.AssertionFailedError;

public class ExternalSlingLaunchpad extends ExternalResource {

    private static final Pattern STARTLEVEL_JSON_SNIPPET = Pattern.compile("\"systemStartLevel\":(\\d+)");
    private static final int EXPECTED_START_LEVEL = 30;
    private static final long MAX_WAIT_TIME_MS = TimeUnit.MINUTES.toMillis(1);
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final LaunchpadConfig config;

    public ExternalSlingLaunchpad(LaunchpadConfig config) {
        this.config = config;
    }

    @Override
    protected void before() throws Throwable {

        Credentials creds = new UsernamePasswordCredentials(config.getUsername(), config.getPassword());

        HttpClient client = new HttpClient();
        client.getState().setCredentials(new AuthScope(config.getHostname(), config.getPort()), creds);
        client.getParams().setAuthenticationPreemptive(true);

        long cutoff = System.currentTimeMillis() + MAX_WAIT_TIME_MS;

        List<SlingReadyRule> rules = new ArrayList<>();
        rules.add(new StartLevelSlingReadyRule(client));
        rules.add(new ActiveBundlesSlingReadyRule(client));
        rules.add(new RepositoryAvailableReadyRule(client));
        
        logger.debug("Starting check");

        for (SlingReadyRule rule : rules) {
            logger.debug("Checking {}", rule);
            while (true) {
                if (rule.evaluate()) {
                    logger.debug("Rule {} succeeded.", rule);
                    break;
                }
                assertTimeout(cutoff);
                Thread.sleep(100);
            }
        }
        
        logger.debug("Checks complete");
    }

    private void assertTimeout(long cutoff) throws AssertionFailedError {
        logger.debug("Checking for timeout, current {}, cutoff {}", System.currentTimeMillis(), cutoff);
        if (System.currentTimeMillis() > cutoff) {
            throw new AssertionFailedError("Sling launchpad did not start within " + MAX_WAIT_TIME_MS + " milliseconds");
        }
    }

    private interface SlingReadyRule {

        boolean evaluate() throws Exception;
    }

    private class StartLevelSlingReadyRule implements SlingReadyRule {

        private final HttpClient client;
        private final GetMethod httpMethod;

        public StartLevelSlingReadyRule(HttpClient client) {
            this.client = client;
            httpMethod = new GetMethod(config.getUrl() + "system/console/vmstat");
        }

        @Override
        public boolean evaluate() throws Exception {

            int status = client.executeMethod(httpMethod);
            logger.debug("vmstat http call got return code {}", status);

            if (status == 200) {

                String responseBody = httpMethod.getResponseBodyAsString();

                Matcher m = STARTLEVEL_JSON_SNIPPET.matcher(responseBody);
                if (m.find()) {
                    int startLevel = Integer.parseInt(m.group(1));
                    logger.debug("vmstat http call got startLevel {}", startLevel);
                    if (startLevel >= EXPECTED_START_LEVEL) {
                        logger.debug("current startLevel {}  >= {}, we are done here", startLevel, EXPECTED_START_LEVEL);
                        return true;
                    }
                }

            }
            return false;
        }
    }

    private class ActiveBundlesSlingReadyRule implements SlingReadyRule {
        private final HttpClient client;
        private final GetMethod httpMethod;

        public ActiveBundlesSlingReadyRule(HttpClient client) {
            this.client = client;
            httpMethod = new GetMethod(config.getUrl() + "system/console/bundles.json");
        }

        @Override
        public boolean evaluate() throws Exception {
            int status = client.executeMethod(httpMethod);
            logger.debug("bundles http call got return code {}", status);
            
            if ( status != 200) {
                return false;
            }

            try (JsonReader jsonReader = new JsonReader(
                    new InputStreamReader(httpMethod.getResponseBodyAsStream(), httpMethod.getResponseCharSet()))) {
                jsonReader.beginObject();
                while (jsonReader.hasNext()) {
                    String name = jsonReader.nextName();
                    if (name.equals("s")) {
                        jsonReader.beginArray();
                        int total = jsonReader.nextInt();
                        int active = jsonReader.nextInt();
                        int fragment = jsonReader.nextInt();
                        logger.debug("bundle http call status: total = {}, active = {}, fragment = {}", total, active, fragment);

                        if (total == active + fragment) {
                            logger.debug("All bundles are started, we are done here");
                            return true;
                        } else {
                            return false;
                        }
                    } else {
                        jsonReader.skipValue();
                    }
                }
            }
            return false;
        }
    }
    
    private class RepositoryAvailableReadyRule implements SlingReadyRule {
        private final HttpClient client;

        public RepositoryAvailableReadyRule(HttpClient client) {
            this.client = client;
        }
        
        @Override
        public boolean evaluate() throws Exception {
            
            for ( String prefix: new String[] { "server", "crx/server"} ) {
                GetMethod get = new GetMethod(config.getUrl() + prefix + "/default/jcr:root/content");
                
                int status = client.executeMethod(get);
                logger.debug("repository check call at entry point {}  got status {}", prefix, status);
                if ( status == 200 ) 
                    return true;
            }
            
            return false;
        }
    }
}

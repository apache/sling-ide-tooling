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

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    	String authorizationHeaderValue = "Basic " + Base64.getEncoder()
                .encodeToString((config.getUsername() + ":" + config.getPassword()).getBytes(StandardCharsets.UTF_8));
        HttpClient client = HttpClient.newBuilder()
                .version(Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        long cutoff = System.currentTimeMillis() + MAX_WAIT_TIME_MS;

        List<SlingReadyRule> rules = new ArrayList<>();
        rules.add(new StartLevelSlingReadyRule(client, authorizationHeaderValue));
        rules.add(new ActiveBundlesSlingReadyRule(client, authorizationHeaderValue));
        rules.add(new RepositoryAvailableReadyRule(client, authorizationHeaderValue));
        
        logger.debug("Starting check");

        for (SlingReadyRule rule : rules) {
            logger.debug("Checking {}", rule);
            while (true) {
                if (rule.evaluate()) {
                    logger.debug("Rule {} succeeded.", rule);
                    break;
                }
                assertTimeout(cutoff, rule);
                Thread.sleep(100);
            }
        }
        
        logger.debug("Checks complete");
    }

    private void assertTimeout(long cutoff, SlingReadyRule rule) throws AssertionFailedError {
        logger.debug("Checking for timeout of rule {}, current {}, cutoff {}", rule, System.currentTimeMillis(), cutoff);
        if (System.currentTimeMillis() > cutoff) {
            throw new AssertionFailedError("Sling launchpad did not fulfill rule " + rule.getClass().getName() + " within " + MAX_WAIT_TIME_MS + " milliseconds" );
        }
    }

    private interface SlingReadyRule {

        boolean evaluate() throws Exception;
    }

    private class StartLevelSlingReadyRule implements SlingReadyRule {

        private final HttpClient client;
        private final HttpRequest request;

        public StartLevelSlingReadyRule(HttpClient client, String authorizationHeaderValue) {
            this.client = client;
            request = HttpRequest.newBuilder()
            		.uri(config.getUrl().resolve("system/console/vmstat"))
            		.header("Authorization", authorizationHeaderValue)
            		.build();
        }

        @Override
        public boolean evaluate() throws Exception {

            HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
            logger.debug("vmstat http call got return code {}", response.statusCode());

            if (response.statusCode() == 200) {


                Matcher m = STARTLEVEL_JSON_SNIPPET.matcher(response.body());
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
        private final HttpRequest request;

        public ActiveBundlesSlingReadyRule(HttpClient client, String authorizationHeaderValue) {
            this.client = client;
            request = HttpRequest.newBuilder()
            		.uri(config.getUrl().resolve("system/console/bundles.json"))
            		.header("Authorization", authorizationHeaderValue)
            		.build();
        }

        @Override
        public boolean evaluate() throws Exception {
        	HttpResponse<InputStream> response = client.send(request, BodyHandlers.ofInputStream());
            logger.debug("bundles http call got return code {}", response.statusCode());
            
            if (response.statusCode() != 200) {
                return false;
            }

            try (JsonReader jsonReader = new JsonReader(
                    new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
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
        private final String authorizationHeaderValue;

        public RepositoryAvailableReadyRule(HttpClient client, String authorizationHeaderValue) {
            this.client = client;
            this.authorizationHeaderValue = authorizationHeaderValue;
        }
        
        @Override
        public boolean evaluate() throws Exception {
            
            for ( String prefix: new String[] { "server", "crx/server"} ) {
            	HttpRequest request = HttpRequest.newBuilder()
                		.uri(config.getUrl().resolve(prefix+"/default/jcr:root/content"))
                		.header("Authorization", authorizationHeaderValue)
                		.build();
                
                HttpResponse<Void> response = client.send(request, BodyHandlers.discarding());
                logger.debug("repository check call at entry point {}  got status {}", prefix, response.statusCode());
                if (response.statusCode() == 200 ) 
                    return true;
            }
            
            return false;
        }
    }
}

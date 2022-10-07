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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.URI;
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
import java.util.stream.Collectors;

import org.junit.rules.ExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;

import junit.framework.AssertionFailedError;

public class ExternalSlingLaunchpad extends ExternalResource {

    private static final Pattern STARTLEVEL_JSON_SNIPPET = Pattern.compile("\"systemStartLevel\":(\\d+)");
    private static final int EXPECTED_START_LEVEL = 30;
    private static final long MAX_WAIT_TIME_MS = TimeUnit.MINUTES.toMillis(1);
    private static final Logger logger = LoggerFactory.getLogger(ExternalSlingLaunchpad.class);

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

        List<ServerReadyGate> gates = new ArrayList<>();
        gates.add(new StartLevelGate(client, authorizationHeaderValue));
        gates.add(new ActiveBundlesGate(logger, client, config.getUrl(), authorizationHeaderValue));
        gates.add(new RepositoryAvailableGate(client, authorizationHeaderValue));
        
        logger.debug("Starting check");

        for (ServerReadyGate gate : gates) {
            logger.debug("Checking {}", gate);
            while (true) {
                if (gate.evaluate()) {
                    logger.debug("Gate {} passed.", gate);
                    break;
                }
                assertTimeout(cutoff, gate);
                Thread.sleep(100);
            }
        }
        
        logger.debug("Checks complete");
    }

    private void assertTimeout(long cutoff, ServerReadyGate gate) throws AssertionFailedError {
        logger.debug("Checking for timeout of gate {}, current {}, cutoff {}", gate, System.currentTimeMillis(), cutoff);
        if (System.currentTimeMillis() > cutoff) {
            throw new AssertionFailedError("Sling server did not pass " + gate.getClass().getName() + " within " + MAX_WAIT_TIME_MS + " milliseconds. It was failing with " + gate.getFailureMessage() );
        }
    }

    private interface ServerReadyGate {

        boolean evaluate() throws Exception;
        String getFailureMessage();
    }

    private class StartLevelGate implements ServerReadyGate {

        private final HttpClient client;
        private final HttpRequest request;
        private int startLevel;

        public StartLevelGate(HttpClient client, String authorizationHeaderValue) {
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
                    startLevel = Integer.parseInt(m.group(1));
                    logger.debug("vmstat http call got startLevel {}", startLevel);
                    if (startLevel >= EXPECTED_START_LEVEL) {
                        logger.debug("current startLevel {}  >= {}, we are done here", startLevel, EXPECTED_START_LEVEL);
                        return true;
                    }
                }

            }
            return false;
        }

		@Override
		public String getFailureMessage() {
			return "Start Level " + startLevel + " was below expected level " + EXPECTED_START_LEVEL;
		}
        
    }

    static class ActiveBundlesGate implements ServerReadyGate {
        private final HttpClient client;
        private final HttpRequest request;
        private String failureMessage;

        public ActiveBundlesGate(HttpClient client, URI baseUrl, String authorizationHeaderValue) {
        	this.client = client;
            request = HttpRequest.newBuilder()
            		.uri(baseUrl.resolve("system/console/bundles.json"))
            		.header("Authorization", authorizationHeaderValue)
            		.build();
        }

        @Override
        public boolean evaluate() throws Exception {
        	HttpResponse<InputStream> response = client.send(request, BodyHandlers.ofInputStream());
            logger.debug("bundles http call got return code {}", response.statusCode());
            
            if (response.statusCode() != 200) {
            	failureMessage = request.toString() + " returned " + response.statusCode();
                return false;
            }

            return areAllBundlesStarted(response.body());
        }

		boolean areAllBundlesStarted(InputStream inputJson) throws IOException {
			try (JsonReader jsonReader = new JsonReader(
                    new InputStreamReader(inputJson, StandardCharsets.UTF_8))) {
                jsonReader.beginObject();
                while (jsonReader.hasNext()) {
                    String name = jsonReader.nextName();
                    if (name.equals("s")) {
                        jsonReader.beginArray();
                        int total = jsonReader.nextInt();
                        int active = jsonReader.nextInt();
                        int fragment = jsonReader.nextInt();
                        jsonReader.nextInt();
                        jsonReader.nextInt();
                        logger.debug("bundle http call status: total = {}, active = {}, fragment = {}", total, active, fragment);
                        if (total == active + fragment) {
                            logger.debug("All bundles are started, we are done here");
                            return true;
                        }
                        jsonReader.endArray();
                    } else if (name.equals("data")) {
                    	Type listType = new TypeToken<List<BundleMetadata>>() {}.getType();
                    	List<BundleMetadata> bundles = new Gson().fromJson(jsonReader, listType);
                    	failureMessage = "The following bundles were not started: " + bundles.stream()
                    		.filter(b -> !(b.state == BundleMetadata.State.ACTIVE ||  b.state == BundleMetadata.State.RESOLVED))
							.map(b -> b.symbolicName + " (" + b.id + ")")
							.collect(Collectors.joining(", "));
                    } else {
                    	jsonReader.skipValue();
                    }
                }
            }
			return false;
		}
        

		@Override
		public String getFailureMessage() {
			return failureMessage;
		}
    }

    protected static final class BundleMetadata {
		private String id;
		private String name;
		private String symbolicName;
		@SerializedName("stateRaw")
		private State state; // https://github.com/apache/felix-dev/blob/de64445723e33c400d4e851f2c2a874beb923119/webconsole/src/main/java/org/apache/felix/webconsole/internal/core/BundlesServlet.java#L438
		
		// https://docs.osgi.org/javadoc/osgi.core/8.0.0/org/osgi/framework/Bundle.html#getState--
		// https://docs.osgi.org/javadoc/osgi.core/8.0.0/constant-values.html#org.osgi.framework.Bundle.ACTIVE
		enum State {
			@SerializedName("1")
			UNINSTALLED,
	
			@SerializedName("2")
			INSTALLED,
	
	        @SerializedName("4")
			RESOLVED,
	
	        @SerializedName("8")
			STARTING,
	
	        @SerializedName("16")
			STOPPING,
			@SerializedName("32")
			ACTIVE;
		}
    }

    private class RepositoryAvailableGate implements ServerReadyGate {
        private final HttpClient client;
        private final String authorizationHeaderValue;

        public RepositoryAvailableGate(HttpClient client, String authorizationHeaderValue) {
            this.client = client;
            this.authorizationHeaderValue = authorizationHeaderValue;
        }
        
        @Override
        public boolean evaluate() throws Exception {
            
            for (String prefix: new String[] { "server", "crx/server"} ) {
            	HttpRequest request = HttpRequest.newBuilder()
                		.uri(config.getUrl().resolve(prefix+"/default/jcr:root/content"))
                		.header("Authorization", authorizationHeaderValue)
                		.build();
                
                HttpResponse<Void> response = client.send(request, BodyHandlers.discarding());
                logger.debug("repository check call at entry point {}  got status {}", prefix, response.statusCode());
                if (response.statusCode() == 200 ) {
                	return true;
                }
            }
            return false;
        }

		@Override
		public String getFailureMessage() {
			return "WebDAV endpoint never returned a 200 status for repository root";
		}
    }
}

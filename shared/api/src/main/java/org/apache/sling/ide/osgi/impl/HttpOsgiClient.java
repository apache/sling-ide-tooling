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
package org.apache.sling.ide.osgi.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeoutException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.AbstractResponseHandler;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.apache.sling.ide.log.Logger;
import org.apache.sling.ide.osgi.OsgiClient;
import org.apache.sling.ide.osgi.OsgiClientException;
import org.apache.sling.ide.osgi.SourceReference;
import org.apache.sling.ide.transport.RepositoryInfo;
import org.osgi.framework.Version;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.TypeAdapter;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

public class HttpOsgiClient implements OsgiClient, AutoCloseable {

	private static final int DEFAULT_SOCKET_TIMEOUT_SECONDS = 30;
	private static final int DEFAULT_CONNECT_TIMEOUT_SECONDS = 15;

	private final RepositoryInfo repositoryInfo;
	private final AuthCache authCache;
	private final Logger logger;
	private final CloseableHttpClient httpClient;

	public HttpOsgiClient(RepositoryInfo repositoryInfo, Logger logger) {
		this.repositoryInfo = repositoryInfo;
		this.logger = logger;

		HttpHost targetHost = new HttpHost(repositoryInfo.getUrl().getHost(), repositoryInfo.getUrl().getPort(), repositoryInfo.getUrl().getScheme());
		CredentialsProvider credsProvider = new BasicCredentialsProvider();
		credsProvider.setCredentials(
				new AuthScope(targetHost),
				new UsernamePasswordCredentials(repositoryInfo.getUsername(), repositoryInfo.getPassword()));
		// Create AuthCache instance
		authCache = new BasicAuthCache();
		// Generate BASIC scheme object and add it to the local auth cache
		BasicScheme basicAuth = new BasicScheme();
		authCache.put(targetHost, basicAuth);
		// set timeouts
		RequestConfig requestConfig = RequestConfig.custom()
				.setConnectTimeout(DEFAULT_CONNECT_TIMEOUT_SECONDS * 1000) 
				.setSocketTimeout(DEFAULT_SOCKET_TIMEOUT_SECONDS * 1000).build();
		httpClient = HttpClients.custom().setDefaultCredentialsProvider(credsProvider)
				.setDefaultRequestConfig(requestConfig).build();
		
	}

	@Override
	public Version getBundleVersion(String bundleSymbolicName) throws OsgiClientException {
		BundleInfo info = getBundleInfo(bundleSymbolicName);
		if (info == null) {
			return null;
		}
		return info.getVersion();
	}

	public HttpClientContext createContextForPreemptiveBasicAuth() {
		// Add AuthCache to the execution context
        HttpClientContext localContext = HttpClientContext.create();
        localContext.setAuthCache(authCache);
		return localContext;
	}

    public void waitForBundleUpdatedAndActive(final String bundleSymbolicName, final long timeout, final long delay, Instant installationTime) throws TimeoutException, InterruptedException {
    	final Instant truncatedInstallationTime = installationTime.truncatedTo(ChronoUnit.SECONDS);
    	Polling p = new Polling() {
            @Override
            public Boolean call() throws Exception {
                BundleInfo info = getBundleInfo(bundleSymbolicName);
                if (info != null) {
                	logger.trace("Bundle {0}, State {1}, Modification date {2}, Installation date {3}", bundleSymbolicName, info.status, info.getLastModification(), truncatedInstallationTime);
                    return (!(info.getLastModification().isBefore(truncatedInstallationTime)) && info.status == BundleInfo.Status.ACTIVE);
                } else {
                    logger.trace("Could not get bundle info for bsn {0}", bundleSymbolicName);
                }
                return false;
            }

            @Override
            protected String message() {
                return "Bundle " + bundleSymbolicName + " was not installed/updated in %1$d ms";
            }
        };
        p.poll(timeout, delay);
    }

    @Override
	public void waitForComponentRegistered(final String componentNameOrId, final long timeout, final long delay) throws TimeoutException, InterruptedException {
        Polling p = new Polling() {
            @Override
            public Boolean call() throws Exception {
                ComponentsInfo info = getComponentsInfo(componentNameOrId);
                if (info != null) {
                    return ((info.components[0].status == ComponentInfo.Status.SATISFIED) || (info.components[0].status == ComponentInfo.Status.ACTIVE));
                } else {
                    logger.trace("Could not get component info for component name {0}", componentNameOrId);
                }
                return false;
            }

            @Override
            protected String message() {
                return "Component " + componentNameOrId + " was not registered in %1$d ms";
            }
        };
        p.poll(timeout, delay);
    }

    /**
     * Returns the wrapper for the component info json
     *
     * @param id the id or name of the component
     * @return the component info or {@code null} if component with the given id/name was not found
     * @throws OSGiClientException
     */
    public ComponentsInfo getComponentsInfo(String componentNameOrId) throws OsgiClientException {
        return executeJsonGetRequest("system/console/components/" + componentNameOrId + ".json", ComponentsInfo.class, "get DS component info", true);
    }
    
    /**
     * Returns the wrapper for the bundle info json
     *
     * @param id the id of the bundle or its symbolic name
     * @return the bundle info or {@code null} if bundle with the given id/symbolicName was not found
     * @throws OSGiClientException
     */
    public BundleInfo getBundleInfo(String bundleSymbolicNameOrId) throws OsgiClientException {
    	BundlesInfo info = executeJsonGetRequest("system/console/bundles/" + bundleSymbolicNameOrId + ".json", BundlesInfo.class, "get bundle info", true);
    	if (info == null) {
    		return null;
    	}
    	return info.bundles[0];
    }

    @Override
	public void installBundle(InputStream in, String bundleSymbolicName) throws OsgiClientException {

		if (in == null) {
			throw new IllegalArgumentException("in may not be null");
		}

		if (bundleSymbolicName == null) {
			throw new IllegalArgumentException("bundleSymbolicName may not be null");
		}

		// append pseudo path after root URL to not get redirected for nothing
		// https://github.com/apache/felix-dev/blob/d55c61712b2bc6ceaa554d1cf99609990355aa4f/webconsole/src/main/java/org/apache/felix/webconsole/internal/core/BundlesServlet.java#L354
		final HttpPost filePost = new HttpPost(repositoryInfo.getUrl().resolve("system/console/install"));

		try {
			// set referrer
			filePost.setHeader("referer", "about:blank");

			MultipartEntityBuilder entityBuilder = MultipartEntityBuilder.create();
			entityBuilder.addTextBody("action", "install");
			entityBuilder.addTextBody("_noredir_", "_noredir_");
			entityBuilder.addTextBody("bundlestart", "start");
			// https://issues.apache.org/jira/browse/FELIX-6585 (the filename is used as location, i.e. should be unique)
			entityBuilder.addBinaryBody("bundlefile", in, ContentType.DEFAULT_BINARY, bundleSymbolicName);

			filePost.setEntity(entityBuilder.build());
			logger.trace("Installing bundle {0} via POST to {1}", bundleSymbolicName, filePost.getURI());
			Instant installationTime = Instant.now();
			httpClient.execute(filePost, new BasicLoggingResponseHandler(), createContextForPreemptiveBasicAuth());
			// due to https://issues.apache.org/jira/browse/FELIX-5562 wait for the install/update
			waitForBundleUpdatedAndActive(bundleSymbolicName, 20000, 500, installationTime);
			
		} catch (IOException e) {
			throw new OsgiClientException("Error installing bundle " + bundleSymbolicName + " via " + filePost, e);
		} catch (TimeoutException|InterruptedException e) {
			throw new OsgiClientException("Error getting status of bundle  " + bundleSymbolicName + " after installation/update ", e);
		}
	}

	@Override
	public boolean uninstallBundle(String bundleSymbolicName) throws OsgiClientException {
		
		HttpPost postMethod = new HttpPost(repositoryInfo.getUrl().resolve("system/console/bundles/" + bundleSymbolicName));
	    try {
			List<? extends NameValuePair> parameters = Collections
					.singletonList(new BasicNameValuePair("action", "uninstall"));
			postMethod.setEntity(new UrlEncodedFormEntity(parameters));
			logger.trace("Uninstalling bundle via {0}", postMethod);
			httpClient.execute(postMethod, new BasicLoggingResponseHandler(),
					createContextForPreemptiveBasicAuth());
			

		} catch (HttpResponseException e) {
			if (e.getStatusCode() == HttpStatus.SC_NOT_FOUND) {
				// deal with 404 (i.e. bundle not found, https://github.com/apache/felix-dev/blob/d55c61712b2bc6ceaa554d1cf99609990355aa4f/webconsole/src/main/java/org/apache/felix/webconsole/internal/core/BundlesServlet.java#L369)
				logger.trace("No bundle found with bsn {0}, skipping uninstallation", bundleSymbolicName);
				return false;
			}
			// TODO: consolidate
			throw new OsgiClientException("Error uninstalling bundle " + bundleSymbolicName + " via " + postMethod);
		} catch (IOException e) {
			throw new OsgiClientException("Error uninstalling bundle " + bundleSymbolicName + " via " + postMethod);
		}
	    return true;
	}

	@Override
	public void installBundle(final Path explodedBundleLocation) throws OsgiClientException {

		if (explodedBundleLocation == null) {
			throw new IllegalArgumentException("explodedBundleLocation may not be null");
		}

		List<? extends NameValuePair> parameters = Collections
				.singletonList(new BasicNameValuePair("dir", explodedBundleLocation.toString()));
		try {
			HttpPost request = new HttpPost(repositoryInfo.getUrl().resolve("system/sling/tooling/install"));
			request.setEntity(new UrlEncodedFormEntity(parameters));
			BundleInstallerResult result = executeJsonRequest(request, BundleInstallerResult.class, "install local bundle from " + explodedBundleLocation.toString(), false);
			if (!result.isSuccessful()) {
				String errorMessage = !result.hasMessage() ? "Bundle deployment failed, please check the Sling logs"
						: result.getMessage();
				throw new OsgiClientException(errorMessage);
			}
		} catch (UnsupportedEncodingException e) {
			throw new OsgiClientException("Cannot install local bundle due to unsupported encoding", e);
		}
	}

	@Override
	public List<SourceReference> findSourceReferences() throws OsgiClientException {
		SourceBundleData[] sourceBundleData = executeJsonGetRequest("system/sling/tooling/sourceReferences.json", SourceBundleData[].class, "find source references", false);
		List<SourceReference> res = new ArrayList<>(sourceBundleData.length);
		for (SourceBundleData sourceData : sourceBundleData) {
			for (SourceReferenceFromJson ref : sourceData.sourceReferences) {
				if (ref.isMavenType()) {
					res.add(ref.getMavenSourceReference());
				}
			}
		}

		return res;
	}

	@Override
	public void close() throws IOException {
		httpClient.close();
	}

	/**
	 * Encapsulates the JSON response from the tooling.installer
	 */
	private static final class BundleInstallerResult {
		private String status; // either OK or FAILURE
		private String message;

		public boolean hasMessage() {
			if (message != null && message.length() > 0) {
				return true;
			}
			return false;
		}

		public String getMessage() {
			return message;
		}

		public boolean isSuccessful() {
			return "OK".equalsIgnoreCase(status);
		}
	}

	private static final class SourceBundleData {

		@SerializedName("Bundle-SymbolicName")
		private String bsn;
		@SerializedName("Bundle-Version")
		private String version;

		private List<SourceReferenceFromJson> sourceReferences;
	}

	private static final class SourceReferenceFromJson {
		@SerializedName("__type__")
		private String type; // should be "maven"
		private String groupId;
		private String artifactId;
		private String version;

		public boolean isMavenType() {
			return "maven".equals(type);
		}

		public MavenSourceReferenceImpl getMavenSourceReference() {
			if (!isMavenType()) {
				throw new IllegalStateException("The type is not a Maven source reference but a " + type);
			}
			return new MavenSourceReferenceImpl(groupId, artifactId, version);
		}
	}

	static final class ComponentsInfo {
		@SerializedName("data") 
		ComponentInfo[] components;
	}

	static final class ComponentInfo {
		String pid;
		String name;
		@SerializedName("state")
		Status status;
		
		public enum Status {

	        // the states being used in the DS Felix WebConsole are listed in https://github.com/apache/felix/blob/6e5cde8471febb36bc72adeba85989edba943188/webconsole-plugins/ds/src/main/java/org/apache/felix/webconsole/plugins/ds/internal/ComponentConfigurationPrinter.java#L374
			@SerializedName("active")
			ACTIVE("active"),

			@SerializedName("satisfied")
	        SATISFIED("satisfied"),

	        @SerializedName("unsatisfied (configuration)")
	        UNSATISFIED_CONFIGURATION("unsatisfied (configuration)"),

	        @SerializedName("unsatisfied (reference)")
	        UNSATISFIED_REFERENCE("unsatisfied (reference)"),

	        @SerializedName("failed activation")
	        FAILED_ACTIVATION("failed activation"),

	        UNKNOWN("unknown");

	        String value;

	        Status(String value) {
	            this.value = value;
	        }

	        public static Status value(String o) {
	            for(Status s : values()) {
	                if(s.value.equalsIgnoreCase(o)) {
	                    return s;
	                }
	            }
	            return UNKNOWN;
	        }

	        public String toString() {
	            return value;
	        }

	    }

	}

	
	static final class BundlesInfo {
		@SerializedName("data") 
		BundleInfo[] bundles;
		
	}
	
	static final class PropertyTypeAdapter extends TypeAdapter<Map<String, Object>> {

		@Override
		public void write(JsonWriter out, Map<String, Object> value) throws IOException {
			throw new UnsupportedOperationException();
		}

		@Override
		public Map<String, Object> read(JsonReader in) throws IOException {
			Map<String, Object> properties = new HashMap<>();
			in.beginArray();
			while (in.peek() == JsonToken.BEGIN_OBJECT) {
				in.beginObject();
				String key = null;
				Object value = null;
				for (int n=0; n<2; n++) {
					String propName = in.nextName();
					if (propName.equals("key")) {
						key = in.nextString();
					} else if (propName.equals("value")) {
						JsonToken token = in.peek();
						if (token == JsonToken.BEGIN_ARRAY) {
							in.beginArray();
							List<Object> list = new LinkedList<>();
							while(in.peek() != JsonToken.END_ARRAY) {
								list.add(extractValue(in));
							}
							in.endArray();
							value = list;
						} else {
							value = extractValue(in);
						}
					} else {
						throw new IOException("Unsupported field in properties" + propName);
					}
				}
				Objects.requireNonNull(key);
				Objects.requireNonNull(value);
				properties.put(key, value);
				in.endObject();
			}
			in.endArray();
			return properties;
		}
		
		private Object extractValue(JsonReader in) throws IOException {
			Object value;
			JsonToken token = in.peek();
			if (token == JsonToken.STRING) {
				value = in.nextString();
			} else if (token == JsonToken.BOOLEAN) {
				value = in.nextBoolean();
			} else if (token == JsonToken.NUMBER) {
				value = in.nextDouble();
			} else {
				// skip nested objects
				in.skipValue();
				value = "Nested array/object";
			}
	
			return value;
		}
		
	}
	
	static final class BundleInfo {
		String id;
		String symbolicName;
		@SerializedName("state")
		Status status;
		private String version;
		@SerializedName("props")
		private Map<String, Object> properties;
		
		/**
		 * Format emitted by {@link java.util.Date#toString()}.
		 */
		static final DateTimeFormatter DATE_TOSTRING_FORMAT = DateTimeFormatter.ofPattern("EEE MMM dd HH:mm:ss zzz yyyy", Locale.ROOT);
		
		public enum Status {

	        // the states being used in the DS Felix WebConsole are listed in https://github.com/apache/felix-dev/blob/d55c61712b2bc6ceaa554d1cf99609990355aa4f/webconsole/src/main/java/org/apache/felix/webconsole/internal/core/BundlesServlet.java#L742
			@SerializedName("Installed")
			INSTALLED("Installed"),

			@SerializedName("Resolved")
	        RESOLVED("Resolved"),

	        @SerializedName("Fragment")
	        FRAGMENT("Fragment"),

	        @SerializedName("Starting")
	        STARTING("Starting"),

	        @SerializedName("Active")
	        ACTIVE("Active"),

	        @SerializedName("Stopping")
	        STOPPING("Stopping"),

	        UNKNOWN("unknown");

	        String value;

	        Status(String value) {
	            this.value = value;
	        }

	        public static Status value(String o) {
	            for(Status s : values()) {
	                if(s.value.equalsIgnoreCase(o)) {
	                    return s;
	                }
	            }
	            return UNKNOWN;
	        }

	        public String toString() {
	            return value;
	        }

	    }
		
		Instant getLastModification() {
			// https://github.com/apache/felix-dev/blob/d55c61712b2bc6ceaa554d1cf99609990355aa4f/webconsole/src/main/java/org/apache/felix/webconsole/internal/core/BundlesServlet.java#L802
			// format as outlined in https://docs.oracle.com/javase/8/docs/api/java/util/Date.html#toString--
			return ZonedDateTime.parse(properties.get("Last Modification").toString(), DATE_TOSTRING_FORMAT).toInstant();
		}
		
		Version getVersion() {
			return new Version(version);
		}

	}

	private <T> T executeJsonGetRequest(String relativePath, Class<T> jsonObjectClass, String requestLabel, boolean returnNullFor404Status) throws OsgiClientException {
		HttpGet request = new HttpGet(repositoryInfo.getUrl().resolve(relativePath));
		return executeJsonRequest(request, jsonObjectClass, requestLabel, returnNullFor404Status);
	}
	
	private <T> T executeJsonRequest(HttpRequestBase request, Class<T> jsonObjectClass, String requestLabel, boolean returnNullFor404Status) throws OsgiClientException {
		try {
			logger.trace("{0} via {1}", requestLabel, request);
			return httpClient.execute(request, new LoggingAbstractResponseHandler<T>() {

				@Override
				public T handleEntity(HttpEntity entity) throws IOException {
					return parseJson(jsonObjectClass, entity.getContent(), getCharsetOrDefault(entity));
				}
				
			}, createContextForPreemptiveBasicAuth());

		} catch (HttpResponseException e) {
			if (returnNullFor404Status && e.getStatusCode() == HttpStatus.SC_NOT_FOUND) {
				return null;
			} else {
				throw new OsgiClientException("Unexpected response code " + e.getStatusCode() + " during " + requestLabel + " via " + request, e);
			}
		} catch (IOException e) {
			throw new OsgiClientException("Cannot " + requestLabel + " via " + request, e);
		}
	}
	

	static <T> T parseJson(Class<T> jsonObjectClass, InputStream input, Charset charset) throws IOException {
		
		GsonBuilder builder = new GsonBuilder();
		builder.registerTypeAdapter(new TypeToken<Map<String, Object>>(){}.getType(), new PropertyTypeAdapter());
	    Gson gson = builder.create();
		try (JsonReader jsonReader = new JsonReader(
				new InputStreamReader(input, charset))) {
			return gson.fromJson(jsonReader, jsonObjectClass);
		} catch (JsonParseException e) {
			throw new IOException("Error parsing JSON response", e);
		}
	}
	
	/**
	 * Slightly extended version of {@link org.apache.http.impl.client.AbstractResponseHandler} which logs the response body in case of status code >= 300.
	 *
	 * @param <T>
	 */
	public abstract class LoggingAbstractResponseHandler<T> implements ResponseHandler<T> {
		/**
		 * Slightly extended version of {@link AbstractResponseHandler#handleResponse(HttpResponse)} which logs the response body for errors
		 * @param response
		 * @return
		 * @throws HttpResponseException
		 * @throws IOException
		 */
	    @Override
	    public T handleResponse(final HttpResponse response)
	            throws HttpResponseException, IOException {
	        final StatusLine statusLine = response.getStatusLine();
	        final HttpEntity entity = response.getEntity();
	        if (statusLine.getStatusCode() >= 300) {
	        	logger.trace("Received failure response " + statusLine  + ":" + EntityUtils.toString(entity));
	            EntityUtils.consume(entity);
	            throw new HttpResponseException(statusLine.getStatusCode(),
	                    statusLine.getReasonPhrase());
	        }
	        return entity == null ? null : handleEntity(entity);
	    }
	    
	    /**
	     * Handle the response entity and transform it into the actual response
	     * object.
	     */
	    public abstract T handleEntity(HttpEntity entity) throws IOException;
	}
	
	public final class BasicLoggingResponseHandler extends LoggingAbstractResponseHandler<String> {

		@Override
		public String handleEntity(HttpEntity entity) throws IOException {
			return EntityUtils.toString(entity);
		}
		
	}
	
	private static Charset getCharsetOrDefault(HttpEntity entity) {
		Charset charset = ContentType.getOrDefault(entity).getCharset();
		if (charset == null) {
			charset = StandardCharsets.ISO_8859_1;
		}
		return charset;
	}
}

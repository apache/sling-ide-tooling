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
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeoutException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.NameValuePair;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.CredentialsProvider;
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
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.sling.ide.log.Logger;
import org.apache.sling.ide.osgi.OsgiClient;
import org.apache.sling.ide.osgi.OsgiClientException;
import org.apache.sling.ide.osgi.SourceReference;
import org.apache.sling.ide.transport.RepositoryInfo;
import org.osgi.framework.Version;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.SerializedName;
import com.google.gson.stream.JsonReader;

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

	private static final class BundleInfo {
		private String symbolicName;
		private String version;
		private long id;

		public String getSymbolicName() {
			return symbolicName;
		}

		public Version getVersion() {
			return new Version(version);
		}

		public long getId() {
			return id;
		}
	}

	private static BundleInfo readBundleInfo(String bundleSymbolicName, Reader reader) throws IOException {
		Gson gson = new Gson();
		try (JsonReader jsonReader = new JsonReader(reader)) {
			// wait for 'data' attribute
			jsonReader.beginObject();
			while (jsonReader.hasNext()) {
				String name = jsonReader.nextName();
				if (name.equals("data")) {
					jsonReader.beginArray();
					while (jsonReader.hasNext()) {
						// read json for individual bundle
						BundleInfo bundleInfo = gson.fromJson(jsonReader, BundleInfo.class);
						if (bundleSymbolicName.equals(bundleInfo.getSymbolicName())) {
							return bundleInfo;
						}
					}
					jsonReader.endArray();
				} else {
					jsonReader.skipValue();
				}
			}
		}
		return null;

	}

	static Version getBundleVersionFromReader(String bundleSymbolicName, Reader reader) throws IOException {
		BundleInfo bundleInfo = readBundleInfo(bundleSymbolicName, reader);
		if (bundleInfo == null) {
			return null;
		}
		return bundleInfo.getVersion();
	}

	static Long getBundleIdFromReader(String bundleSymbolicName, Reader reader) throws IOException {
		BundleInfo bundleInfo = readBundleInfo(bundleSymbolicName, reader);
		if (bundleInfo == null) {
			return null;
		}
		return bundleInfo.getId();
	}

	@Override
	public Version getBundleVersion(String bundleSymbolicName) throws OsgiClientException {

		HttpGet method = new HttpGet(repositoryInfo.getUrl().resolve("system/console/bundles.json"));

		try {
			return httpClient.execute(method, new AbstractResponseHandler<Version>() {

				@Override
				public Version handleEntity(HttpEntity entity) throws IOException {
					try (InputStream input = entity.getContent();
							Reader reader = new InputStreamReader(input, StandardCharsets.US_ASCII)) {
						return getBundleVersionFromReader(bundleSymbolicName, reader);
					}
				}

			}, createContextForPreemptiveBasicAuth());

		} catch (IOException e) {
			throw new OsgiClientException(e);
		}
	}

	public HttpClientContext createContextForPreemptiveBasicAuth() {
		// Add AuthCache to the execution context
        HttpClientContext localContext = HttpClientContext.create();
        localContext.setAuthCache(authCache);
		return localContext;
	}

//	/**
//     * Wait until the service with the given name is registered. This means the component must be either in state "Registered" or "Active".
//     * @param type the type of the service (usually the name of a Java interface)
//     * @param bundleSymbolicName the symbolic name of the bundle supposed to register that service.
//     * May be {@code null} in which case this method just waits for any service with the requested type being registered (independent of the registering bundle).
//     * @param timeout how long to wait for the component to become registered before throwing a {@code TimeoutException} in milliseconds
//     * @param delay time to wait between checks of the state in milliseconds
//     * @throws TimeoutException if the component did not become registered before timeout was reached
//     * @throws InterruptedException if interrupted
//     */
//    public void waitForServiceRegistered(final String type, final String bundleSymbolicName, final long timeout, final long delay) throws TimeoutException, InterruptedException {
//        Polling p = new Polling() {
//            @Override
//            public Boolean call() throws Exception {
//                Collection<ServiceInfo> infos = getServiceInfos(type);
//                if (infos != null) {
//                    if (bundleSymbolicName != null) {
//                        for (ServiceInfo info : infos) {
//                            if (bundleSymbolicName.equals(info.getBundleSymbolicName())) {
//                                return true;
//                            }
//                        }
//                        LOG.debug("Could not find service info for service type {} provided by bundle {}", type, bundleSymbolicName);
//                        return false;
//                    } else {
//                        return !infos.isEmpty();
//                    }
//                } else {
//                    LOG.debug("Could not find any service info for service type {}", type);
//                }
//                return false;
//            }
//
//            @Override
//            protected String message() {
//                return "Service with type " + type + " was not registered in %1$d ms";
//            }
//        };
//        p.poll(timeout, delay);
//    }
//
//    
//    /**
//     * Returns the service info wrapper for all services implementing the given type.
//     *
//     * @param name the type of the service
//     * @return the service infos or {@code null} if no service for the given type is registered
//     */
//    private Collection<ServiceInfo> getServiceInfos(String type) throws ClientException {
//        SlingHttpResponse resp = this.doGet(URL_SERVICES + ".json");
//        if (HttpUtils.getHttpStatus(resp) == SC_OK) {
//            return new ServicesInfo(JsonUtils.getJsonNodeFromString(resp.getContent())).forType(type);
//        }
//        return null;
//    }
    
    @Override
	public void waitForComponentRegistered(final String componentName, final long timeout, final long delay) throws TimeoutException, InterruptedException {
        Polling p = new Polling() {
            @Override
            public Boolean call() throws Exception {
                ComponentsInfo info = getComponentsInfo(componentName);
                if (info != null) {
                    return ((info.components[0].status == ComponentInfo.Status.SATISFIED) || (info.components[0].status == ComponentInfo.Status.ACTIVE));
                } else {
                    logger.trace("Could not get component info for component name {0}", componentName);
                }
                return false;
            }

            @Override
            protected String message() {
                return "Component " + componentName + " was not registered in %1$d ms";
            }
        };
        p.poll(timeout, delay);
    }

    /**
     * Returns the wrapper for the component info json
     *
     * @param id the id of the component
     * @return the component info
     * @throws ClientException if the response status does not match any of the expectedStatus
     */
    public ComponentsInfo getComponentsInfo(String id) throws OsgiClientException {
        return executeJsonGetRequest("system/console/components/" + id + ".json", ComponentsInfo.class);
    }
    
    @Override
	public void installBundle(InputStream in, String fileName) throws OsgiClientException {

		if (in == null) {
			throw new IllegalArgumentException("in may not be null");
		}

		if (fileName == null) {
			throw new IllegalArgumentException("fileName may not be null");
		}

		// append pseudo path after root URL to not get redirected for nothing
		final HttpPost filePost = new HttpPost(repositoryInfo.getUrl().resolve("system/console/install"));

		try {
			// set referrer
			filePost.setHeader("referer", "about:blank");

			MultipartEntityBuilder entityBuilder = MultipartEntityBuilder.create();
			entityBuilder.addTextBody("action", "install");
			entityBuilder.addTextBody("_noredir_", "_noredir_");
			entityBuilder.addTextBody("bundlestart", "start");
			entityBuilder.addBinaryBody("bundlefile", in, ContentType.DEFAULT_BINARY, "bundle.jar");

			filePost.setEntity(entityBuilder.build());
			logger.trace("Installing bundle {0} via POST to {1}", fileName, filePost.getURI());
			httpClient.execute(filePost, new BasicResponseHandler(), createContextForPreemptiveBasicAuth());
		} catch (IOException e) {
			throw new OsgiClientException(e);
		}
	}

	@Override
	public void uninstallBundle(String bundleSymbolicName) throws OsgiClientException {
		HttpGet method = new HttpGet(repositoryInfo.getUrl().resolve("system/console/bundles.json"));

		try {
			logger.trace("Retrieving bundle id for bsn {0} via GET to {1}", bundleSymbolicName, method.getURI());
			Long bundleId = httpClient.execute(method, new AbstractResponseHandler<Long>() {

				@Override
				public Long handleEntity(HttpEntity entity) throws IOException {
					try (Reader reader = new InputStreamReader(entity.getContent(), StandardCharsets.US_ASCII)) {
						return getBundleIdFromReader(bundleSymbolicName, reader);
					}
				}

			}, createContextForPreemptiveBasicAuth());

			HttpPost postMethod = new HttpPost(repositoryInfo.getUrl().resolve("system/console/bundles/" + bundleId));
			List<? extends NameValuePair> parameters = Collections
					.singletonList(new BasicNameValuePair("action", "uninstall"));
			postMethod.setEntity(new UrlEncodedFormEntity(parameters));
			logger.trace("Uninstalling bundle via POST to {0}", postMethod.getURI());
			httpClient.execute(postMethod, new BasicResponseHandler(),
					createContextForPreemptiveBasicAuth());

		} catch (IOException e) {
			throw new OsgiClientException(e);
		}
	}

	@Override
	public void installLocalBundle(final Path explodedBundleLocation) throws OsgiClientException {

		if (explodedBundleLocation == null) {
			throw new IllegalArgumentException("explodedBundleLocation may not be null");
		}

		List<? extends NameValuePair> parameters = Collections
				.singletonList(new BasicNameValuePair("dir", explodedBundleLocation.toString()));
		try {
			installLocalBundle(new UrlEncodedFormEntity(parameters));
		} catch (UnsupportedEncodingException e) {
			throw new OsgiClientException(e);
		}
	}

	@Override
	public void installLocalBundle(final InputStream jarredBundle, String sourceLocation) throws OsgiClientException {

		if (jarredBundle == null) {
			throw new IllegalArgumentException("jarredBundle may not be null");
		}

		MultipartEntityBuilder entityBuilder = MultipartEntityBuilder.create();
		entityBuilder.addBinaryBody("bundle", jarredBundle, ContentType.DEFAULT_BINARY, "bundle.jar");
		installLocalBundle(entityBuilder.build());
	}

	@Override
	public List<SourceReference> findSourceReferences() throws OsgiClientException {
		SourceBundleData[] sourceBundleData = executeJsonGetRequest("system/sling/tooling/sourceReferences.json", SourceBundleData[].class);
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
	public void close() throws Exception {
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

	void installLocalBundle(HttpEntity httpEntity) throws OsgiClientException {
		logger.trace("Installing local bundle...");
		HttpPost request = new HttpPost(repositoryInfo.getUrl().resolve("system/sling/tooling/install"));
		request.setEntity(httpEntity);
		BundleInstallerResult result = executeJsonRequest(request, BundleInstallerResult.class);
		if (!result.isSuccessful()) {
			String errorMessage = !result.hasMessage() ? "Bundle deployment failed, please check the Sling logs"
					: result.getMessage();
			throw new OsgiClientException(errorMessage);
		}
	}

	private <T> T executeJsonGetRequest(String relativePath, Class<T> jsonObjectClass) throws OsgiClientException {
		HttpGet request = new HttpGet(repositoryInfo.getUrl().resolve(relativePath));
		return executeJsonRequest(request, jsonObjectClass);
	}
	
	private <T> T executeJsonRequest(HttpRequestBase request, Class<T> jsonObjectClass) throws OsgiClientException {
		try {
			logger.trace("Executing {0} to {1}", request.getMethod(), request.getURI());
			return httpClient.execute(request, new AbstractResponseHandler<T>() {

				@Override
				public T handleEntity(HttpEntity entity) throws IOException {
					return parseJson(jsonObjectClass, entity.getContent());
				}
				
			}, createContextForPreemptiveBasicAuth());

		} catch (IOException e) {
			throw new OsgiClientException(e);
		}
	}
	

	static <T> T parseJson(Class<T> jsonObjectClass, InputStream input) throws IOException {
		try (JsonReader jsonReader = new JsonReader(
				new InputStreamReader(input, StandardCharsets.UTF_8))) {
			return new Gson().fromJson(jsonReader, jsonObjectClass);
		} catch (JsonParseException e) {
			throw new IOException("Error parsing JSON response", e);
		}
	}
}

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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

import javax.jcr.Credentials;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.jackrabbit.util.Text;
import org.apache.sling.ide.jcr.RepositoryUtils;
import org.apache.sling.ide.transport.RepositoryInfo;
import org.hamcrest.CoreMatchers;

/**
 * The <tt>RepositoryAccessor</tt> makes it simple to access and validate the contents of a Sling repository during
 * testing
 *
 */
public class RepositoryAccessor {

    private final LaunchpadConfig config;
    private final HttpClient client;
    private final String encodedAuth;
    private Repository repository;
    private Credentials credentials;

    public RepositoryAccessor(LaunchpadConfig config) {
        this.config = config;
        encodedAuth = Base64.getEncoder()
                .encodeToString((config.getUsername() + ":" + config.getPassword()).getBytes(StandardCharsets.UTF_8));
        client = HttpClient.newBuilder()
                .version(Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    HttpRequest.Builder customizeRequest(HttpRequest.Builder builder) {
    	return builder.header("Authorization", "Basic " + encodedAuth);
    }

    public void assertGetIsSuccessful(String path, String expectedResult) throws IOException, InterruptedException {
    	HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
    	         .uri(config.getUrl().resolve(path));
    	HttpRequest request = customizeRequest(requestBuilder).build();
        HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
        assertThat("Unexpected status call for " + request, response.statusCode(), CoreMatchers.equalTo(200));
    
        assertThat("Unexpected response for " + request, response.body(),
                    CoreMatchers.equalTo(expectedResult));
        
    }

    public void assertGetReturns404(String path) throws IOException, InterruptedException {

    	HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
   	         .uri(config.getUrl().resolve(path));
    	HttpRequest request = customizeRequest(requestBuilder).build();
        HttpResponse<Void> response = client.send(request, BodyHandlers.discarding());
        assertThat("Unexpected status call for " + request, response.statusCode(), CoreMatchers.equalTo(404));
    }

    public void tryDeleteResource(String path) throws RepositoryException {

        // PostMethod pm = new PostMethod(config.getUrl() + "hello.txt");
        // Part[] parts = { new StringPart(":operation", "delete") };
        // pm.setRequestEntity(new MultipartRequestEntity(parts, pm.getParams()));
        // try {
        // client.executeMethod(pm);
        // } finally {
        // pm.releaseConnection();
        // }

        Session session = login();
        if (session.nodeExists(path)) {
            session.removeItem(path);
            session.save();
        }
    }

    public Node getNode(String nodePath) throws RepositoryException {

        return login().getNode(nodePath);
    }

    /**
     * Returns true if a node exists at the specified path
     * 
     * @param path the path, in absolute format or relative to the repository root
     * @return true if the path exists, false otherwise
     * @throws RepositoryException
     */
    public boolean hasNode(String path) throws RepositoryException {
        if (path.startsWith("/")) {
            path = path.substring(1);
        }

        return login().getRootNode().hasNode(path);
    }

    public void createNode(String path, String primaryNodeType) throws RepositoryException {

        Session session = login();
        try {
            if (session.nodeExists(path)) {
                return;
            }

            Node parent = session.getNode(Text.getRelativeParent(path, 1));
            parent.addNode(Text.getName(path), primaryNodeType);
            session.save();
        } finally {
            session.logout();
        }
    }

    public void createFile(String path, byte[] bytes) throws RepositoryException {
        Session session = login();
        try {
            if (session.nodeExists(path)) {
                return;
            }

            Node parent = session.getNode(Text.getRelativeParent(path, 1));
            Node resourceNode = parent.addNode(Text.getName(path), "nt:file");
            Node contentNode = resourceNode.addNode("jcr:content", "nt:resource");
            contentNode
                    .setProperty("jcr:data", session.getValueFactory().createBinary(new ByteArrayInputStream(bytes)));
            session.save();
        } finally {
            session.logout();
        }
    }
    
    /**
     * Executes a user-specified <tt>runnable</tt> on the repository
     * 
     * <p>
     * All exceptions are propagated as they happen. It is the responsibility of the runnable to call
     * <tt>session.save()</tt> to persist the changes.
     * </p>
     * 
     * @param runnable
     * @return the result of the runnable's execution
     * @throws RepositoryException any exception that occurs when executing the runnable
     */
    public <T> T doWithSession(SessionRunnable<T> runnable) throws RepositoryException {

        Session session = login();
        try {
            return runnable.doWithSession(session);
        } finally {
            session.logout();
        }
    }

    private Session login() throws RepositoryException {
        
        RepositoryInfo repositoryInfo = new RepositoryInfo(config.getUsername(), config.getPassword(), config.getUrl());

        if (repository == null) {
            repository = RepositoryUtils.getRepository(repositoryInfo);
        }

        if (credentials == null) {
            credentials = RepositoryUtils.getCredentials(repositoryInfo);
        }
        
        return repository.login(credentials);
    }


    public interface SessionRunnable<T> {
        public T doWithSession(Session session) throws RepositoryException;
    }
}

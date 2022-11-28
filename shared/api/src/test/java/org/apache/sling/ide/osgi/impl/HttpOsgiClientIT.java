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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeoutException;
import java.util.function.Predicate;

import org.apache.commons.io.file.PathUtils;
import org.apache.sling.ide.osgi.MavenSourceReference;
import org.apache.sling.ide.osgi.OsgiClient;
import org.apache.sling.ide.osgi.OsgiClientException;
import org.apache.sling.ide.osgi.SourceReference;
import org.apache.sling.ide.transport.RepositoryInfo;
import org.apache.sling.ide.util.Slf4jLogger;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.osgi.framework.Version;

public class HttpOsgiClientIT {

	private OsgiClient osgiClient;

	@Rule
	public TemporaryFolder tmpFolder = new TemporaryFolder();
	
	@Before
	public void setUp() throws URISyntaxException {
		int port = Objects.requireNonNull(Integer.getInteger("sling.starter.port"), "System property 'sling.starter.port' must be set to a valid integer");
    	URI url = new URI("http", null, "localhost", port, "/", null, null);
    	RepositoryInfo repoInfo = new RepositoryInfo("admin", "admin", url);
        osgiClient = new HttpOsgiClient(repoInfo, new Slf4jLogger());
	}
	
	@After
	public void tearDown() throws Exception {
		osgiClient.close();
	}
	
    @Test
    public void testGetBundleVersion() throws IOException, URISyntaxException, OsgiClientException {
        assertEquals(new Version("2.24.0"), osgiClient.getBundleVersion("org.apache.sling.api"));
        assertNull(osgiClient.getBundleVersion("invalid-bsn"));
    }

    @Test
    public void testInstallBundle() throws IOException, URISyntaxException, OsgiClientException, InterruptedException, TimeoutException {
    	// create an exploded jar folder
    	URI jarUri = new URI("jar:" + this.getClass().getResource("/org.apache.sling.commons.messaging.jar").toString());
    	
    	Path explodedJarFolder = tmpFolder.newFolder("exploded-jar").toPath();
    	try (FileSystem zipfs = FileSystems.newFileSystem(jarUri, Collections.emptyMap())) {
    		for (Path srcRootDirectory : zipfs.getRootDirectories()) {
    			PathUtils.copyDirectory(srcRootDirectory, explodedJarFolder);
    		}
        }
    	// first install the necessary tooling
    	try (InputStream input = Objects.requireNonNull(this.getClass().getResourceAsStream("/org.apache.sling.tooling.support.install.jar"))) {
    		osgiClient.installBundle(input, "org.apache.sling.tooling.support.install");
    	}
    	osgiClient.waitForComponentRegistered("org.apache.sling.tooling.support.install.impl.InstallServlet", 20000, 500);
    	// install another bundle from JAR
    	try (InputStream input = Objects.requireNonNull(this.getClass().getResourceAsStream("/org.apache.sling.tooling.support.source.jar"))) {
    		osgiClient.installBundle(input, "org.apache.sling.tooling.support.source");
    	}
    	// check that both are installed
    	assertNotNull(osgiClient.getBundleVersion("org.apache.sling.tooling.support.install"));
    	assertNotNull(osgiClient.getBundleVersion("org.apache.sling.tooling.support.source"));
    	
    	osgiClient.installBundle(explodedJarFolder);
    	assertNotNull(osgiClient.getBundleVersion("org.apache.sling.commons.messaging"));
    	assertNotNull(osgiClient.getBundleVersion("org.apache.sling.tooling.support.install"));
    	assertNotNull(osgiClient.getBundleVersion("org.apache.sling.tooling.support.source"));
    	
        assertTrue(osgiClient.uninstallBundle("org.apache.sling.tooling.support.install"));
        assertTrue(osgiClient.uninstallBundle("org.apache.sling.tooling.support.source"));
    }
    
    @Test
    public void testFindSourceReferences() throws IOException, URISyntaxException, OsgiClientException, InterruptedException, TimeoutException {
    	// first install the necessary tooling
    	try (InputStream input = Objects.requireNonNull(this.getClass().getResourceAsStream("/org.apache.sling.tooling.support.source.jar"))) {
    		osgiClient.installBundle(input, "org.apache.sling.tooling.support.source");
    	}
    	osgiClient.waitForComponentRegistered("org.apache.sling.tooling.support.source.impl.SourceReferencesServlet", 20000, 500);
    	List<SourceReference> sourceReferences = osgiClient.findSourceReferences();
    	Optional<SourceReference> source = sourceReferences.stream().filter(new MavenSourceRefenceMatchingPredicate("org.apache.sling", "org.apache.sling.api")).findFirst();
    	assertTrue(source.isPresent());
    }
    
    @Test
    public void testWaitForComponentRegistered() throws TimeoutException, InterruptedException {
    	assertThrows(TimeoutException.class, () -> osgiClient.waitForComponentRegistered("invalidComponentName", 1000, 100));
    	osgiClient.waitForComponentRegistered("1", 1000, 100);
    }

    private static final class MavenSourceRefenceMatchingPredicate implements Predicate<SourceReference> {
    	private final String groupId;
    	private final String artifactId;
    	public MavenSourceRefenceMatchingPredicate(String groupId, String artifactId) {
    		this.groupId = groupId;
    		this.artifactId = artifactId;
    	}
    	
		@Override
		public boolean test(SourceReference source) {
			if (source instanceof MavenSourceReference) {
    			MavenSourceReference mavenSource = (MavenSourceReference)source;
    			if (mavenSource.getGroupId().equals(groupId) &&
    				mavenSource.getArtifactId().equals(artifactId)) {
    				return true;
    			}
    		}
			return false;
		}
    }
}

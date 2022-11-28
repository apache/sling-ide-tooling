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
package org.apache.sling.ide.osgi;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeoutException;

import org.osgi.framework.Version;

/**
 * The <tt>OsgiClient</tt> exposes information and actions related to the OSGi subsystem of Sling.
 * It leverages ReST services exposed from the 
 * <ol>
 * <li><a href="https://felix.apache.org/documentation/subprojects/apache-felix-web-console/web-console-restful-api.html">Felix Web Console</a></li>
 * <li><a href="https://github.com/apache/sling-org-apache-sling-tooling-support-install">Sling Tooling Support Install</a></li>
 * <li><a href="https://github.com/apache/sling-org-apache-sling-tooling-support-source">Sling Tooling Support Source</a></li>
 * </ol>
 */
public interface OsgiClient extends AutoCloseable {

	/**
	 * Gets the version of an installed bundle.
	 * 
	 * @param bundleSymbolicName the BSN of the bundle whose version to retrieve
	 * @return the installed version or {@code null} if not found
	 * @throws OsgiClientException
	 */
    Version getBundleVersion(String bundleSymbolicName) throws OsgiClientException;

    /**
     * Installs/Updates a bundle from a JAR synchronously (i.e. after the call returns the bundle has been installed/updated).
     * It updates an existing bundle in case a bundle with the same symbolic name does already exist.
     * <p>
     * Leverages the <a href="https://felix.apache.org/documentation/subprojects/apache-felix-web-console/web-console-restful-api.html">Felix Web Console ReST endpoint</a>.
     * @param in the contents of the jarred bundle
     * @param bundleSymbolicName the bundle symbolic name
     * 
     * @throws OsgiClientException
     */
    void installBundle(InputStream in, String bundleSymbolicName) throws OsgiClientException;

    /**
     * Installs/Updates a bundle from a local filesystem directory synchronously (i.e. after the call returns the bundle has been installed/updated)
     * It updates an existing bundle in case a bundle with the same symbolic name does already exist.
     * 
     * <strong>The Sling launchpad instance must have filesystem access to the specified <tt>explodedBundleLocation</tt></strong>
     * <p>
     * Leverages the <a href="https://github.com/apache/sling-org-apache-sling-tooling-support-install">Sling Tooling Support Install bundle</a>.
     * 
     * @param explodedBundleLocation the local path of the directory containing the exploded bundle
     * @throws OsgiClientException
     */
    void installBundle(Path explodedBundleLocation) throws OsgiClientException;
    
    /**
     * Finds source references for all bundles deployed in the Sling instance
     * <p>
     * Leverages the <a href="https://github.com/apache/sling-org-apache-sling-tooling-support-source">Sling Tooling Support Source bundle</a>.
     * @return the source references, possibly empty
     * @throws OsgiClientException
     */
    List<SourceReference> findSourceReferences() throws OsgiClientException;
    
    /**
     * Uninstalls the bundle with the specified Bundle-SymbolicName, if present
     * <p>
     * Leverages the <a href="https://felix.apache.org/documentation/subprojects/apache-felix-web-console/web-console-restful-api.html">Felix Web Console ReST endpoint</a>.
     * @param bundleSymbolicName The Bundle-SymbolicName
     * @return true in case a bundle with that BSN was found and uninstalled, false in case the BSN was not found
     * @throws OsgiClientException error when trying to uninstall the bundle
     */
    boolean uninstallBundle(String bundleSymbolicName) throws OsgiClientException;

	/**
	 * Wait until the component with the given name is registered. This means the component must be either in state "Registered" or "Active".
	 * The state registered is called "satisfied" in the Felix DS Web Console
	 * @param componentNameOrId the component's name (by default the FQCN) or its id
	 * @param timeout how long to wait for the component to become registered before throwing a {@code TimeoutException} in milliseconds
	 * @param delay time to wait between checks of the state in milliseconds
	 * @throws TimeoutException if the component did not become registered before timeout was reached
	 * @throws InterruptedException if interrupted
	 * @see "OSGi Comp. R6, §112.5 Component Life Cycle"
	 */
	void waitForComponentRegistered(final String componentNameOrId, final long timeout, final long delay)
			throws TimeoutException, InterruptedException;

	void close() throws IOException;
}
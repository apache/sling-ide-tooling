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
package org.apache.sling.ide.artifacts.impl;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.apache.sling.ide.artifacts.EmbeddedBundle;
import org.apache.sling.ide.artifacts.EmbeddedBundleLocator;
import org.osgi.service.component.annotations.Component;

@Component(service = EmbeddedBundleLocator.class)
public class EmbeddedBundleLocatorImpl implements EmbeddedBundleLocator {

	private final Map<String, EmbeddedBundle> bundles;
	public EmbeddedBundleLocatorImpl() throws IOException {
		bundles = new HashMap<>();
		bundles.put(SUPPORT_INSTALL_BUNDLE_SYMBOLIC_NAME, getArtifactFromResource("org.apache.sling.tooling.support.install"));
		bundles.put(SUPPORT_SOURCE_BUNDLE_SYMBOLIC_NAME, getArtifactFromResource("org.apache.sling.tooling.support.source"));
	}


	private EmbeddedBundle getArtifactFromResource(String artifactId) throws IOException {
    	URL jarUrl = loadResource(artifactId + ".jar");
        return new EmbeddedBundle(jarUrl);
    }

    private URL loadResource(String resourceLocation) {
        URL resourceUrl = this.getClass().getClassLoader().getResource(resourceLocation);
        if (resourceUrl == null) {
            throw new RuntimeException("Unable to locate bundle resource " + resourceLocation);
        }
        return resourceUrl;
    }

    
	@Override
	public EmbeddedBundle getBundle(String bundleSymbolicName) throws IOException {
		EmbeddedBundle bundle = bundles.get(bundleSymbolicName);
		if (bundle == null) {
			throw new IllegalArgumentException("The bundle with bsn " + bundleSymbolicName + " is not provided by this locator");
		}
		return bundle;
	}

}

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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.apache.sling.ide.artifacts.EmbeddedBundle;
import org.apache.sling.ide.artifacts.EmbeddedBundleLocator;
import org.junit.Test;
import org.osgi.framework.Version;

public class EmbeddedBundlesLocatorTest {

	@Test
	public void testLoadSourceSupportBundle() throws IOException {
		EmbeddedBundleLocatorImpl locator = new EmbeddedBundleLocatorImpl();
		EmbeddedBundle bundle = locator.getBundle(EmbeddedBundleLocator.SUPPORT_SOURCE_BUNDLE_SYMBOLIC_NAME);
		assertEmbeddedArtifact(bundle, "org.apache.sling.tooling.support.source.jar");
		assertEquals(new Version("1.1.0.SNAPSHOT"), bundle.getVersion());
		assertEquals(EmbeddedBundleLocator.SUPPORT_SOURCE_BUNDLE_SYMBOLIC_NAME, bundle.getBundleSymbolicName());
	}

	@Test
	public void testLoadInstallSupportBundle() throws IOException {
		EmbeddedBundleLocatorImpl locator = new EmbeddedBundleLocatorImpl();
		EmbeddedBundle bundle = locator.getBundle(EmbeddedBundleLocator.SUPPORT_INSTALL_BUNDLE_SYMBOLIC_NAME);
		assertEmbeddedArtifact(bundle, "org.apache.sling.tooling.support.install.jar");
		assertEquals(new Version("1.1.0.SNAPSHOT"), bundle.getVersion());
		assertEquals(EmbeddedBundleLocator.SUPPORT_INSTALL_BUNDLE_SYMBOLIC_NAME, bundle.getBundleSymbolicName());
	}

	private void assertEmbeddedArtifact(EmbeddedBundle bundle, String expectedInputResourceName) throws IOException {
		try (InputStream expectedInput = this.getClass().getClassLoader().getResourceAsStream(expectedInputResourceName);
			InputStream actualInput = bundle.openInputStream()) {
			assertTrue(IOUtils.contentEquals(expectedInput, actualInput));
		}
	}
}

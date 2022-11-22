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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertThrows;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

import org.apache.sling.ide.osgi.MavenSourceReference;
import org.apache.sling.ide.osgi.SourceReference;
import org.apache.sling.ide.osgi.impl.HttpOsgiClient.BundlesInfo;
import org.apache.sling.ide.osgi.impl.HttpOsgiClient.ComponentInfo;
import org.apache.sling.ide.osgi.impl.HttpOsgiClient.ComponentsInfo;
import org.junit.Assert;
import org.junit.Test;
import org.osgi.framework.Version;

public class HttpOsgiClientTest {

   
    
    @Test
    public void testComponentsInfoJson() throws IOException {
    	try (InputStream input = Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream("components.json"))) {
    		ComponentsInfo componentsInfo = HttpOsgiClient.parseJson(ComponentsInfo.class, input, StandardCharsets.UTF_8);
    		assertEquals(1, componentsInfo.components.length);
    		assertEquals(ComponentInfo.Status.ACTIVE, componentsInfo.components[0].status);
    		assertEquals("org.apache.sling.tooling.support.install.impl.InstallServlet", componentsInfo.components[0].pid);
    	}
    }
    
    @Test
    public void testBundlesInfoJson() throws IOException {
    	try (InputStream input = Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream("bundleDetails.json"))) {
    		BundlesInfo info = HttpOsgiClient.parseJson(BundlesInfo.class, input, StandardCharsets.UTF_8);
    		assertEquals(1, info.bundles.length);
    		assertEquals(Instant.parse("2022-11-11T11:12:07Z"), info.bundles[0].getLastModification());
    	}
    }
}

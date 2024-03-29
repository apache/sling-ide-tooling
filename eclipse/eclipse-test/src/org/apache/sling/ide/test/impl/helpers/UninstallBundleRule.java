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

import org.apache.sling.ide.osgi.OsgiClient;
import org.apache.sling.ide.osgi.OsgiClientException;
import org.apache.sling.ide.test.impl.Activator;
import org.apache.sling.ide.transport.RepositoryInfo;
import org.junit.rules.ExternalResource;

public class UninstallBundleRule extends ExternalResource {

    private final LaunchpadConfig config;
    private String bundleSymbolicName;

    public UninstallBundleRule(LaunchpadConfig config, String bundleSymbolicName) {
        this.config = config;
        this.bundleSymbolicName = bundleSymbolicName;
    }
    
    @Override
    protected void before() throws Throwable {
        after();
    }
    
    @Override
    protected void after() {
    	RepositoryInfo repositoryInfo = new RepositoryInfo(config.getUsername(), config.getPassword(), config.getUrl());
        try (OsgiClient client = Activator.getDefault().getOsgiClientFactory().createOsgiClient(repositoryInfo)) {
            client.uninstallBundle(bundleSymbolicName);
            new Poller().pollUntilTrue(() -> {
               return client.getBundleVersion(bundleSymbolicName) == null; 
            });
        } catch (OsgiClientException|IOException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

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

import org.apache.sling.ide.osgi.OsgiClient;
import org.apache.sling.ide.osgi.OsgiClientFactory;
import org.apache.sling.ide.transport.RepositoryInfo;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.EventAdmin;

@Component(service = OsgiClientFactory.class)
public class HttpOsgiClientFactory implements OsgiClientFactory {

    @Reference
    private EventAdmin eventAdmin;

    public HttpOsgiClientFactory() {
    }

    /**
     * Constructor to create this instance outside of an OSGi Container
     *
     * @param eventAdmin Event Admin for tracing the OSGi Client. If null then there is no tracing.
     */
    public HttpOsgiClientFactory(EventAdmin eventAdmin) {
        bindEventAdmin(eventAdmin);
    }

    public OsgiClient createOsgiClient(RepositoryInfo repositoryInfo) {
        if (eventAdmin != null) {
            return new TracingOsgiClient(new HttpOsgiClient(repositoryInfo), eventAdmin);
        }
        return new HttpOsgiClient(repositoryInfo);
    }

    protected void bindEventAdmin(EventAdmin eventAdmin) {
        this.eventAdmin = eventAdmin;
    }

    protected void unbindEventAdmin(EventAdmin eventAdmin) {
        this.eventAdmin = null;
    }
}
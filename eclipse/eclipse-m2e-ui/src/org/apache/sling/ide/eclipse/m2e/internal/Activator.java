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
package org.apache.sling.ide.eclipse.m2e.internal;

import org.apache.sling.ide.artifacts.EmbeddedArtifactLocator;
import org.apache.sling.ide.log.Logger;
import org.apache.sling.ide.osgi.OsgiClientFactory;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.osgi.framework.BundleContext;

public class Activator extends Plugin {

    public static final String PLUGIN_ID = "org.apache.sling.ide.eclipse-m2e-ui";
    public static Activator INSTANCE;

    /**
     * Storage for preferences.
     */
    private ScopedPreferenceStore preferenceStore;

    public static Activator getDefault() {
        return INSTANCE;
    }

    @Override
    public void start(BundleContext context) throws Exception {
        super.start(context);

        INSTANCE = this;
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        INSTANCE = null;

        super.stop(context);
    }

    public EmbeddedArtifactLocator getArtifactLocator() {
        return org.apache.sling.ide.eclipse.core.internal.Activator.getDefault().getArtifactLocator();
    }

    public OsgiClientFactory getOsgiClientFactory() {
        return org.apache.sling.ide.eclipse.core.internal.Activator.getDefault().getOsgiClientFactory();
    }

    public Logger getPluginLogger() {
        return org.apache.sling.ide.eclipse.core.internal.Activator.getDefault().getPluginLogger();
    }
    
    public IPreferenceStore getPreferenceStore() {
        // Create the preference store lazily.
        if (preferenceStore == null) {
            preferenceStore = new ScopedPreferenceStore(InstanceScope.INSTANCE, PLUGIN_ID);

        }
        return preferenceStore;
    }
}

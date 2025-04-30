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
package org.apache.sling.ide.eclipse.core.internal;

import java.util.Optional;

import org.apache.sling.ide.artifacts.EmbeddedBundleLocator;
import org.apache.sling.ide.eclipse.core.ExtendedServiceTracker;
import org.apache.sling.ide.eclipse.core.Preferences;
import org.apache.sling.ide.eclipse.core.launch.SourceReferenceResolver;
import org.apache.sling.ide.filter.FilterLocator;
import org.apache.sling.ide.log.Logger;
import org.apache.sling.ide.osgi.OsgiClientFactory;
import org.apache.sling.ide.serialization.SerializationManager;
import org.apache.sling.ide.sync.content.SyncCommandFactory;
import org.apache.sling.ide.transport.BatcherFactory;
import org.apache.sling.ide.transport.RepositoryFactory;
import org.eclipse.core.runtime.Plugin;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 * 
 * <p>
 * Since the WST framework is based on Eclipse extension points, rather than OSGi services, this class provides a static
 * entry point to well-known services.
 * </p>
 */
public class Activator extends Plugin {

    // The plug-in ID
    public static final String PLUGIN_ID = "org.apache.sling.ide.eclipse-core"; //$NON-NLS-1$

	// The shared instance
	private static Activator plugin;

    private ExtendedServiceTracker<RepositoryFactory> repositoryFactory;
    private ExtendedServiceTracker<SerializationManager> serializationManager;
    private ExtendedServiceTracker<FilterLocator> filterLocator;
    private ExtendedServiceTracker<OsgiClientFactory> osgiClientFactory;
    private ExtendedServiceTracker<EmbeddedBundleLocator> bundleLocator;
    private ExtendedServiceTracker<Logger> tracer;
    private ExtendedServiceTracker<BatcherFactory> batcherFactoryLocator;
    private ExtendedServiceTracker<SourceReferenceResolver> sourceReferenceLocator;
    private ExtendedServiceTracker<SyncCommandFactory> commandFactory;
    
    private Preferences preferences;
    
    public static final String BSN_VAULT_IMPL = "org.apache.sling.ide.impl-vlt";
    public static final String BSN_API = "org.apache.sling.ide.api";
    public static final String BSN_ARTIFACTS = "org.apache.sling.ide.artifacts";

	public void start(BundleContext context) throws Exception {
        super.start(context);
        plugin = this;
        repositoryFactory = new ExtendedServiceTracker<>(context, RepositoryFactory.class);
        serializationManager = new ExtendedServiceTracker<>(context, SerializationManager.class);
        filterLocator = new ExtendedServiceTracker<>(context, FilterLocator.class);
        osgiClientFactory = new ExtendedServiceTracker<>(context, OsgiClientFactory.class);
        bundleLocator = new ExtendedServiceTracker<>(context, EmbeddedBundleLocator.class);
        tracer = new ExtendedServiceTracker<>(context, Logger.class);
        batcherFactoryLocator = new ExtendedServiceTracker<>(context, BatcherFactory.class);
        sourceReferenceLocator = new ExtendedServiceTracker<>(context, SourceReferenceResolver.class);
        commandFactory = new ExtendedServiceTracker<>(context, SyncCommandFactory.class);
	}

	static Bundle getFirstBundle(BundleContext bundleContext, String bundleSymbolicName)
	{
		for (Bundle bundle : bundleContext.getBundles()) {
			if (bundleSymbolicName.equals(bundle.getSymbolicName())) {
				return bundle;
			}
		}
		throw new IllegalStateException("Bundle with Bundle-SymbolicName '" + bundleSymbolicName + "' could not be found. Something went wrong during installation");
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
	    
        repositoryFactory.close();
        serializationManager.close();
        filterLocator.close();
        osgiClientFactory.close();
        bundleLocator.close();
        tracer.close();
        batcherFactoryLocator.close();
        sourceReferenceLocator.close();
        commandFactory.close();

        plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static Activator getDefault() {
		return plugin;
	}

    public RepositoryFactory getRepositoryFactory() {
        return repositoryFactory.getNotNull();
	}

    public SerializationManager getSerializationManager() {
        return serializationManager.getNotNull();
    }

    public FilterLocator getFilterLocator() {
        return filterLocator.getNotNull();
    }

    public OsgiClientFactory getOsgiClientFactory() {
        return osgiClientFactory.getNotNull();
    }

    public EmbeddedBundleLocator getEmbeddedBundleLocator() {
        return bundleLocator.getNotNull();
    }

    public Logger getPluginLogger() {
        return tracer.getNotNull();
    }
    
    public BatcherFactory getBatcherFactory() {
        return batcherFactoryLocator.getNotNull();
    }
    
    public SyncCommandFactory getCommandFactory() {
        return commandFactory.getNotNull();
    }
    
    /**
     * @return the source reference resolver, possibly null
     */
    public Optional<SourceReferenceResolver> getSourceReferenceResolver() {
        return sourceReferenceLocator.getOptional();
    }
    
    public Preferences getPreferences() {
        // Create the preferences lazily.
        if (preferences == null) {
            preferences = new Preferences();
        }
        return preferences;
    }
}

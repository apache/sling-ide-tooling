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
package org.apache.sling.ide.eclipse.core.debug;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.sling.ide.eclipse.core.debug.impl.Tracer;
import org.apache.sling.ide.log.Logger;
import org.eclipse.osgi.service.debug.DebugOptions;
import org.eclipse.osgi.service.debug.DebugOptionsListener;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.framework.ServiceRegistration;

/**
 * The <tt>PluginLoggerRegistrar</tt> registers {@link Logger} implementations for use for specific plugins
 * 
 * <p>This implementation acts as an extender, looking for the header {@value #HEADER_NAME_LOGGER_ENABLED} in
 * the bundle's manifest. If presented and with a value of <tt>true</tt>, a <tt>Logger</tt> instance is registered
 * for that bundle.</p>
 * 
 * <p>In turn, <tt>ServiceRegistration</tt> objects can be accessed using the {@link #getServiceRegistration(Bundle)}.
 * This method and the static {@link #getInstance()}} method are present to make it easier to consume in Eclipse
 * plug-ins, where working with declarative services is more complicated.</p>
 *
 */
public class PluginLoggerRegistrar implements BundleListener {
    
    private static final String HEADER_NAME_LOGGER_ENABLED = "SlingIDE-PluginLoggerEnabled";
    
    private static final PluginLoggerRegistrar INSTANCE = new PluginLoggerRegistrar();
    
    public static PluginLoggerRegistrar getInstance() {
        return INSTANCE;
    }
    
    private final ConcurrentMap<Long, ServiceRegistration<Logger>> registrations = new ConcurrentHashMap<>();
    
    private PluginLoggerRegistrar() {
        
    }

    public void init(Bundle[] bundles) {
        for ( Bundle bundle : bundles )
            if ( bundle.getState() == Bundle.ACTIVE || bundle.getState() == Bundle.STARTING)
                registerIfEnabled(bundle);
    }
    
    public void shutdown() {
        for ( ServiceRegistration<Logger> reg : registrations.values() ) 
            reg.unregister();
    }
    
    /**
     * Gets the service registartion of a <tt>Logger</tt> for the specified bundle
     * 
     * <p>The client must <b>not</b> call <tt>unregister</tt> on the returned instance, as
     * it is owned by the <tt>PluginLoggerRegistrar</tt>.</p>
     * 
     * @param bundle the bundle to get the service registartion for for
     * @return the service registration, or <code>null</code> if none was found for this bundle
     */
    public ServiceRegistration<Logger> getServiceRegistration(Bundle bundle) {
        return registrations.get(bundle.getBundleId());
    }
    
    @Override
    public void bundleChanged(BundleEvent evt) {
        switch ( evt.getType() ) {
            case BundleEvent.STARTED:
                registerIfEnabled(evt.getBundle());
                break;
                
            case BundleEvent.STOPPING:
                unregister(evt.getBundle());
                break;
        }
    }

    private void unregister(Bundle bundle) {
        ServiceRegistration<Logger> reg = registrations.remove(bundle.getBundleId());
        if ( reg != null )
            reg.unregister();
        
    }

    private void registerIfEnabled(final Bundle bundle) {
        if ( Boolean.parseBoolean(bundle.getHeaders().get(HEADER_NAME_LOGGER_ENABLED)) )
            registrations.put(bundle.getBundleId(), register(bundle));
    }
    
    /**
     * Registers a new tracer for the specified bundle
     * 
     * @param bundle the bundle to register for
     * @return the service registration
     */
    private ServiceRegistration<Logger> register(Bundle bundle) {

        Dictionary<String, Object> props = new Hashtable<>();
        props.put(DebugOptions.LISTENER_SYMBOLICNAME, bundle.getSymbolicName());
        BundleContext ctx = bundle.getBundleContext();
        
        // safe to downcast since we are registering the Tracer which implements Logger
        @SuppressWarnings("unchecked")
        ServiceRegistration<Logger> serviceRegistration = (ServiceRegistration<Logger>) ctx.registerService(new String[] { DebugOptionsListener.class.getName(), Logger.class.getName() },
                new Tracer(bundle), props);
        
        return serviceRegistration;
    }
    
}

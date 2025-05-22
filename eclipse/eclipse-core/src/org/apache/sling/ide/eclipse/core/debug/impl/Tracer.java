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
package org.apache.sling.ide.eclipse.core.debug.impl;

import java.util.Date;

import org.apache.sling.ide.eclipse.core.logger.LogSubscriber;
import org.eclipse.osgi.service.debug.DebugOptions;
import org.eclipse.osgi.service.debug.DebugOptionsListener;
import org.eclipse.osgi.service.debug.DebugTrace;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;

/**
 * The <tt>Tracer</tt> is the default implementation of the <tt>Logger</tt>.
 * 
 */
@Component(property = DebugOptions.LISTENER_SYMBOLICNAME + "="
		+ Tracer.BUNDLE_SYMBOLIC_NAME, service = LogSubscriber.class)
public class Tracer implements DebugOptionsListener, LogSubscriber {

	protected static final String BUNDLE_SYMBOLIC_NAME = "org.apache.sling.ide.eclipse-core";
    private boolean debugEnabled;
    private boolean consoleEnabled;
    private boolean performanceEnabled;
    private DebugTrace trace;

    private ServiceRegistration<DebugOptionsListener> debugOptionsListenerRegistration;

    @Activate
    public void activate(BundleContext context) {
        // defer registration of the DebugOptionsListener service until this component is activated, otherwise this bundle is eagerly loaded by Equinox
        context.registerService(DebugOptionsListener.class, this, null);
    }

    @Deactivate
    public void deactivate(BundleContext context) {
        debugOptionsListenerRegistration.unregister();
    }

    @Override
    public void optionsChanged(DebugOptions options) {
        debugEnabled = options.getBooleanOption(BUNDLE_SYMBOLIC_NAME + "/debug", false);
        consoleEnabled = options.getBooleanOption(BUNDLE_SYMBOLIC_NAME + "/debug/console", false) && debugEnabled;
        performanceEnabled = options.getBooleanOption(BUNDLE_SYMBOLIC_NAME + "/debug/performance", false) && debugEnabled;
        trace = options.newDebugTrace(BUNDLE_SYMBOLIC_NAME, getClass());
    }
    
    private void writeToStdOut(String message, Throwable t) {
        System.out.println("[" + Thread.currentThread().getName() + "] " + new Date() + " "
                + " : " + message);
        if (t != null)
            t.printStackTrace(System.out);
    }

	@Override
	public void log(Severity severity, String message, Throwable t) {
		if (!debugEnabled) {
			return;
		}
		if (consoleEnabled && severity == Severity.TRACE) {
			trace.trace("/debug", message);
			writeToStdOut(message, t);
		} else if (performanceEnabled && severity == Severity.TRACE) {
			trace.trace("/debug/performance", message);
			writeToStdOut(message, t);
		}
	}

}

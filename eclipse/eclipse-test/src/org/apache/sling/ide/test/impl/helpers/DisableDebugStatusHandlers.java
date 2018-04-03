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

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.internal.core.IInternalDebugCoreConstants;
import org.junit.rules.ExternalResource;

/**
 * The <tt>DisableDebugStatusHandlers</tt> prevents status prompts, since they can lead to the test Eclipse instance
 * hanging
 *
 */
public class DisableDebugStatusHandlers extends ExternalResource {

    @Override
    protected void before() throws Throwable {
        IEclipsePreferences debugPrefs = InstanceScope.INSTANCE.getNode(DebugPlugin.getUniqueIdentifier());
        debugPrefs.putBoolean(IInternalDebugCoreConstants.PREF_ENABLE_STATUS_HANDLERS, false);
        debugPrefs.flush();
    }
}

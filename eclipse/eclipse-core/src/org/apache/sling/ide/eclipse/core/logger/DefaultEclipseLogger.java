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
package org.apache.sling.ide.eclipse.core.logger;

import org.apache.sling.ide.eclipse.core.internal.Activator;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.osgi.service.component.annotations.Component;

@Component
public class DefaultEclipseLogger implements LogSubscriber {

	@Override
	public void log(Severity severity, String message, Throwable cause) {
		final int statusCode;
		switch(severity) {
		case ERROR:
			statusCode = IStatus.ERROR;
			break;
		case WARNING:
			statusCode = IStatus.WARNING;
			break;
		default:
			return;
		}
		Platform.getLog(Activator.getDefault().getBundle()).log(new Status(statusCode, Activator.getDefault().getBundle().getSymbolicName(), message, cause));
	}

}

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

import java.util.List;

import org.apache.sling.ide.log.Logger;
import org.eclipse.osgi.util.NLS;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicyOption;

@Component
public class CompositeLogger implements Logger {

	private static final long PERF_IGNORE_THRESHOLD = 50;

	@Reference(policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.AT_LEAST_ONE)
	List<LogSubscriber> logSubscribers;

	@Override
	public void trace(String message, Object... arguments) {
		if (arguments.length > 0) {
			message = NLS.bind(message, arguments);
		}
		logInternal(LogSubscriber.Severity.TRACE, message);
	}

	@Override
	public void trace(String message, Throwable error) {
		logInternal(LogSubscriber.Severity.TRACE, message, error);
	}

	@Override
	public void warn(String message) {
		logInternal(LogSubscriber.Severity.WARNING, message);
	}

	@Override
	public void warn(String message, Throwable cause) {
		logInternal(LogSubscriber.Severity.WARNING, message, cause);
	}

	@Override
	public void error(String message) {
		logInternal(LogSubscriber.Severity.ERROR, message);
	}

	@Override
	public void error(String message, Throwable cause) {
		logInternal(LogSubscriber.Severity.ERROR, message, cause);
	}

	@Override
	public void tracePerformance(String message, long duration, Object... arguments) {
		if (duration < PERF_IGNORE_THRESHOLD) {
			return;
		}

		if (arguments.length > 0) {
			message = NLS.bind(message, arguments);
		}
		String fullMessage = message + " took " + duration + " ms";
		logInternal(LogSubscriber.Severity.TRACE_PERFORMANCE, fullMessage);
	}

	private void logInternal(LogSubscriber.Severity severity, String message) {
		logInternal(severity, message, null);
	}

	private void logInternal(LogSubscriber.Severity severity, String message, Throwable cause) {
		for (LogSubscriber logSubscriber : logSubscribers) {
			logSubscriber.log(severity, message, cause);
		}
	}
}

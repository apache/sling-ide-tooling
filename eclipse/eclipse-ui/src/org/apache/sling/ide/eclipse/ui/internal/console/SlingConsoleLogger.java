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
package org.apache.sling.ide.eclipse.ui.internal.console;

import java.io.IOException;
import java.io.PrintStream;
import java.time.Instant;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import org.apache.sling.ide.eclipse.core.logger.LogSubscriber;
import org.apache.sling.ide.eclipse.ui.console.SlingConsoleFactory;
import org.apache.sling.ide.eclipse.ui.internal.Activator;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleListener;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;

@Component // cannot use DS 1.4 property types due to
			// https://github.com/eclipse-pde/eclipse.pde/issues/36)
public class SlingConsoleLogger implements LogSubscriber {

	private final Object sync = new Object();

	private IConsoleManager consoleManager = ConsolePlugin.getDefault().getConsoleManager();
	private MessageConsole slingConsole;
	private IConsoleListener listener;
	
	@Activate
	public void activate() {
		consoleManager = ConsolePlugin.getDefault().getConsoleManager();
		listener = new IConsoleListener() {

			@Override
			public void consolesRemoved(IConsole[] consoles) {
				synchronized (sync) {
					for (IConsole console : consoles) {
						if (console.equals(slingConsole)) {
							slingConsole = null;
						}
					}
				}
			}

			@Override
			public void consolesAdded(IConsole[] consoles) {
				synchronized (sync) {
					for (IConsole console : consoles) {
						if (console.getType().equals(SlingConsoleFactory.CONSOLE_TYPE_SLING)) {
							slingConsole = (MessageConsole) console;
							break;
						}
					}
				}
			}
		};

		consoleManager.addConsoleListener(listener);
	}

	@Deactivate
	public void deactivate() {
		consoleManager.removeConsoleListener(listener);
	}

	@Override
	public void log(Severity severity, String message, Throwable t) {
		synchronized (sync) {
			initSlingConsole();
			if (slingConsole != null) {
				log(severity, message, t, slingConsole);
			}
		}
	}

	private void initSlingConsole() {
		if (slingConsole == null) {
			for (IConsole console : consoleManager.getConsoles()) {
				if (console.getType().equals(SlingConsoleFactory.CONSOLE_TYPE_SLING)) {
					slingConsole = (MessageConsole) console;
					return;
				}
			}
		}
	}

	private void log(Severity severity, String message, Throwable t, MessageConsole console) {
		try (MessageConsoleStream messageStream = console.newMessageStream()) {

			StringBuilder msgBuilder = new StringBuilder();
			
			msgBuilder.append("[").append(DateTimeFormatter.ISO_LOCAL_TIME.format(LocalTime.now())).append("] ").append(message);
			messageStream.write(message.toString());
			if (t != null) {
				t.printStackTrace(new PrintStream(messageStream));
			}
		} catch (IOException e) {
			Activator.getDefault().getPluginLogger().warn("Failed writing to the console", e);
		}
	}

}

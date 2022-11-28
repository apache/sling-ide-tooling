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
package org.apache.sling.ide.artifacts;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

import org.osgi.framework.Constants;
import org.osgi.framework.Version;

public class EmbeddedBundle {

    private final String bundleSymbolicName;
    private final Version version;
    private final URL source;

    public EmbeddedBundle(URL source) throws IOException {
    	try (JarInputStream jarInput = new JarInputStream(source.openStream())) {
        	Manifest manifest = jarInput.getManifest();
        	this.version = new Version(manifest.getMainAttributes().getValue(Constants.BUNDLE_VERSION));
        	this.bundleSymbolicName = manifest.getMainAttributes().getValue(Constants.BUNDLE_SYMBOLICNAME);
        }
    	this.source = source;
    }
    

    public String getBundleSymbolicName() {
        return bundleSymbolicName;
    }

    public Version getVersion() {
        return version;
    }

    /**
     * 
     * Returns a new input stream to this embedded artifact
     * 
     * <p>
     * It is the responsibility of the caller to close this input stream when no longer needed.
     * </p>
     * 
     * @return an input stream to this source
     * @throws IOException
     */
    public InputStream openInputStream() throws IOException {

        return source.openStream();
    }

}

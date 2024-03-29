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

public interface EmbeddedBundleLocator {

    public static final String SUPPORT_INSTALL_BUNDLE_SYMBOLIC_NAME = "org.apache.sling.tooling.support.install";
    
    public static final String SUPPORT_SOURCE_BUNDLE_SYMBOLIC_NAME = "org.apache.sling.tooling.support.source";

    /**
     * 
     * @param bundleSymbolicName
     * @return the embedded bundle (never {@code null}).
     * @throws IOException in case the embedded bundle could not be read
     * @throws IllegalArgumentException in case the bundle with the given bundleSymbolicName is not provided by this locator
     */
    EmbeddedBundle getBundle(String bundleSymbolicName) throws IOException;
}

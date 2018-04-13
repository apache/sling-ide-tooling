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
package org.apache.sling.ide.sync.content;

import java.nio.file.Path;

/**
 * Groups common operations on workspace resources.
 */
public interface WorkspaceResource {
    
    /**
     * Returns the name of this resource
     * 
     * @return the name of the resource
     */
    default String getName() {
        return getLocalPath().getName();
    }
    
    /**
     * Returns true if the resource exists, false otherwise
     * 
     * <p>A resource object not exist if it is constructed for instance to
     * send a notification for a deleted resource.</p>
     * 
     * @return true if the resource exists, false otherwise
     */
    boolean exists();
    
    /**
     * Returns true if this resource is ignored for content sync purposes
     * 
     * @return true if the resource is ignored, false otherwise
     */
    boolean isIgnored();
    
    /**
     * Returns the absolute local path, rooted at the workspace level
     * 
     * @return the local path
     */
    WorkspacePath getLocalPath();
    
    /**
     * Returns the absolute OS path to this resource
     * 
     * @return the OS path
     */
    Path getOSPath();
    
    /**
     * @return the project which holds this resource
     */
    WorkspaceProject getProject();
    
    /**
     * @return the last modified timestamp
     */
    long getLastModified();
    
    /**
     * Returns a value for the specified transient property
     * 
     * <p>The properties are not persisted and the lifetime of the duration
     * is governed by the specific implementation.</p>
     * 
     * @param propertyName property name
     * @return the value for the transient property, or <code>null</code>
     */
    Object getTransientProperty(String propertyName);
    
    default WorkspacePath getPathRelativeToSyncDir() {
        final WorkspacePath relativePath = getProject().getSyncDirectory().getLocalPath().relativize(getLocalPath());
        if ( relativePath == null )
            throw new RuntimeException("Unable to get relative path between sync dir " + getProject().getSyncDirectory().getLocalPath() + " and " + getLocalPath());
        return relativePath;
    }
}

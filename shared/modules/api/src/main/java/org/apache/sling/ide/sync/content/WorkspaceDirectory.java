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

import java.util.List;

/**
 * Represents a local directory, <tt>i.e.</tt> present in the local workspace
 *
 */
public interface WorkspaceDirectory extends WorkspaceResource {

    /**
     * Returns a child file for this directory.
     * 
     * <p>The file may not exist, so make sure to check {@link WorkspaceResource#exists()}</p>
     * 
     * @param relativePath the relative path to the file
     * @return the file ( which may not exist )
     */
    WorkspaceFile getFile(WorkspacePath relativePath);

    /**
     * Returns a child directory for this directory.
     * 
     * <p>The directory may not exist, so make sure to check {@link WorkspaceResource#exists()}</p>
     * 
     * @param relativePath the relative path to the directory
     * @return the directory ( which may not exist )
     */        
    WorkspaceDirectory getDirectory(WorkspacePath relativePath);

    /**
     * Returns a list of children, guaranteed to exist at the time of the call
     * 
     * @return a list of children
     */
    List<WorkspaceResource> getChildren();
}

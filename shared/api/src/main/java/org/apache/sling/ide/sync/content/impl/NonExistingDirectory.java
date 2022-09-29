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
package org.apache.sling.ide.sync.content.impl;

import java.nio.file.Path;
import java.util.List;

import org.apache.sling.ide.sync.content.WorkspaceDirectory;
import org.apache.sling.ide.sync.content.WorkspaceFile;
import org.apache.sling.ide.sync.content.WorkspacePath;
import org.apache.sling.ide.sync.content.WorkspacePaths;
import org.apache.sling.ide.sync.content.WorkspaceProject;
import org.apache.sling.ide.sync.content.WorkspaceResource;

public class NonExistingDirectory implements WorkspaceDirectory {

    private final WorkspacePath path;
    private final WorkspaceProject project;
    
    public NonExistingDirectory(WorkspacePath path, WorkspaceProject project) {
        this.path = path;
        this.project = project;
    }

    @Override
    public boolean exists() {
        return false;
    }

    @Override
    public boolean isIgnored() {
        return false;
    }

    @Override
    public WorkspacePath getLocalPath() {
        return path;
    }

    @Override
    public Path getOSPath() {
        return WorkspacePaths.toOsPath(path);
    }

    @Override
    public WorkspaceProject getProject() {
        return project;
    }

    @Override
    public long getLastModified() {
        throw new IllegalArgumentException("Directory at " + path + " does not exist");
    }

    @Override
    public Object getTransientProperty(String propertyName) {
        throw new IllegalArgumentException("Directory at " + path + " does not exist");
    }

    @Override
    public WorkspaceFile getFile(WorkspacePath relativePath) {
        throw new IllegalArgumentException("Directory at " + path + " does not exist");
    }

    @Override
    public WorkspaceDirectory getDirectory(WorkspacePath relativePath) {
        throw new IllegalArgumentException("Directory at " + path + " does not exist");
    }

    @Override
    public List<WorkspaceResource> getChildren() {
        throw new IllegalArgumentException("Directory at " + path + " does not exist");
    }

}

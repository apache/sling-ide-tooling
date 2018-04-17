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
package org.apache.sling.ide.content.sync.fs.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.sling.ide.sync.content.WorkspaceDirectory;
import org.apache.sling.ide.sync.content.WorkspaceFile;
import org.apache.sling.ide.sync.content.WorkspacePath;
import org.apache.sling.ide.sync.content.WorkspaceProject;

public class FSWorkspaceFile extends FSWorkspaceResource implements WorkspaceFile {
    
    private final FSWorkspaceProject project;
    private final WorkspacePath path;

    public FSWorkspaceFile(File file, FSWorkspaceProject project) {
        super(file, false);
        this.project = project;
        this.path = getPath(project, file);
    }

    @Override
    public boolean isIgnored() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public WorkspacePath getLocalPath() {
        return path;
    }

    @Override
    public WorkspaceProject getProject() {
        return project;
    }

    @Override
    public Object getTransientProperty(String propertyName) {
        return null;
    }

    @Override
    public InputStream getContents() throws IOException {
        return new FileInputStream(backingFile());
    }

    @Override
    public WorkspaceDirectory getParent() {
        return new FSWorkspaceDirectory(backingFile().getParentFile(), project);
    }

}

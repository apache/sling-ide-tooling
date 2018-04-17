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
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.sling.ide.sync.content.WorkspaceDirectory;
import org.apache.sling.ide.sync.content.WorkspaceFile;
import org.apache.sling.ide.sync.content.WorkspacePath;
import org.apache.sling.ide.sync.content.WorkspaceProject;
import org.apache.sling.ide.sync.content.WorkspaceResource;

public class FSWorkspaceDirectory extends FSWorkspaceResource implements WorkspaceDirectory {

    private final FSWorkspaceProject project;
    private final WorkspacePath path;

    public FSWorkspaceDirectory(File dir, FSWorkspaceProject project) {
        super(dir, true);
        this.project = project;
        this.path = getPath(project, dir);
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
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public WorkspaceFile getFile(WorkspacePath relativePath) {
        return new FSWorkspaceFile(new File(backingFile(), relativePath.asPortableString().replace('/', File.separatorChar)), project);
    }

    @Override
    public WorkspaceDirectory getDirectory(WorkspacePath relativePath) {
        return new FSWorkspaceDirectory(new File(backingFile(), relativePath.asPortableString().replace('/', File.separatorChar)), project);
    }

    @Override
    public List<WorkspaceResource> getChildren() {
        return Arrays.stream(backingFile().listFiles())
                .map( f -> {
                    if ( f.isFile() )
                        return new FSWorkspaceFile(f, project);
                    else if ( f.isDirectory() )
                        return new FSWorkspaceDirectory(f, project);
                    else return null;
                })
                .filter( r -> r != null)
                .collect(Collectors.toList());
    }

}

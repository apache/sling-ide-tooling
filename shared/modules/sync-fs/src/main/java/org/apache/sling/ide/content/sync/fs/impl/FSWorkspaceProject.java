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

import org.apache.sling.ide.filter.Filter;
import org.apache.sling.ide.filter.FilterLocator;
import org.apache.sling.ide.sync.content.NonExistingResources;
import org.apache.sling.ide.sync.content.WorkspaceDirectory;
import org.apache.sling.ide.sync.content.WorkspacePath;
import org.apache.sling.ide.sync.content.WorkspacePaths;
import org.apache.sling.ide.sync.content.WorkspaceProject;

public class FSWorkspaceProject extends FSWorkspaceResource implements WorkspaceProject {

    private final File syncRoot;
    private final FilterLocator filterLocator;
    
    public FSWorkspaceProject(File projectDir, File syncRoot, FilterLocator filterLocator) {
        super(projectDir, true);
        this.syncRoot = syncRoot;
        this.filterLocator = filterLocator;
    }

    @Override
    public boolean isIgnored() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public WorkspacePath getLocalPath() {
        // by definition the project path is '/' + the folder name
        return new WorkspacePath('/' + backingFile().getName());
    }

    @Override
    public WorkspaceProject getProject() {
        return this;
    }


    @Override
    public Object getTransientProperty(String propertyName) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public WorkspaceDirectory getSyncDirectory() {
        return new FSWorkspaceDirectory(new File(syncRoot, "jcr_root"), this);
    }

    @Override
    public Filter getFilter() throws IOException {
        File filterFile = filterLocator.findFilterLocation(new File(syncRoot, "jcr_root"));
        if ( filterFile == null || !filterFile.exists() )
            return null;
        
        try ( InputStream filter = new FileInputStream(filterFile) ) {
            return filterLocator.loadFilter(filter);
        }
    }

    @Override
    public WorkspaceDirectory getDirectory(WorkspacePath path) {
        final File osFile = new File(backingFile(), WorkspacePaths.toOsPath(path).toString());
        if ( !osFile.isDirectory() )
            return NonExistingResources.newDirectory(getLocalPath().append(path), this);
        return new FSWorkspaceDirectory(osFile, this);
    }

}

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
import java.nio.file.Path;
import java.util.Objects;

import org.apache.sling.ide.sync.content.WorkspacePath;
import org.apache.sling.ide.sync.content.WorkspacePaths;
import org.apache.sling.ide.sync.content.WorkspaceProject;
import org.apache.sling.ide.sync.content.WorkspaceResource;

public abstract class FSWorkspaceResource implements WorkspaceResource {
    
    static WorkspacePath getPath(WorkspaceProject project, File file){
        return WorkspacePaths.fromOsPath(project.getOSPath().getParent().relativize(file.toPath())).absolute();
    }

    private final File backingFile;

    protected FSWorkspaceResource(File backingFile, boolean shouldBeDirectory) {
        if ( backingFile == null )
            throw new IllegalArgumentException("dir is null");
        if ( backingFile.exists() ) {
            if ( shouldBeDirectory && !backingFile.isDirectory())
                    throw new IllegalArgumentException("File '" + backingFile + "' is not a directory");
            if ( !shouldBeDirectory && !backingFile.isFile())
                throw new IllegalArgumentException("File '" + backingFile + "' is not a regular file");
        }
        
        this.backingFile = backingFile;
        
    }
    
    protected File backingFile() {
        return backingFile;
    }

    @Override
    public boolean exists() {
        return backingFile.exists();
    }

    @Override
    public boolean isIgnored() {
        return false;
    }

    @Override
    public Path getOSPath() {
        return backingFile.toPath();
    }

    @Override
    public long getLastModified() {
        return backingFile.lastModified();
    }

    @Override
    public Object getTransientProperty(String propertyName) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int hashCode() {
        return Objects.hash(backingFile);
    }

    @Override
    public boolean equals(Object obj) {
        
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        FSWorkspaceResource other = (FSWorkspaceResource) obj;
        
        return Objects.equals(backingFile, other.backingFile);
    }
}

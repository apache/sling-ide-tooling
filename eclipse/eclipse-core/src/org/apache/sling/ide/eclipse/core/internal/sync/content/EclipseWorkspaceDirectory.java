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
package org.apache.sling.ide.eclipse.core.internal.sync.content;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.sling.ide.eclipse.core.EclipseResources;
import org.apache.sling.ide.sync.content.WorkspaceDirectory;
import org.apache.sling.ide.sync.content.WorkspaceFile;
import org.apache.sling.ide.sync.content.WorkspaceProject;
import org.apache.sling.ide.sync.content.WorkspaceResource;
import org.apache.sling.ide.sync.content.WorkspacePath;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.CoreException;

public class EclipseWorkspaceDirectory extends EclipseWorkspaceResource implements WorkspaceDirectory {

    public EclipseWorkspaceDirectory(IFolder folder, Set<String> ignoredFileNames) {
        super(folder, ignoredFileNames);
    }
    
    @Override
    protected IFolder getResource() {
        return (IFolder) super.getResource();
    }
    
    @Override
    public WorkspaceProject getProject() {
        return new EclipseWorkspaceProject(getResource().getProject(), getIgnoredFileNames());
    }
    
    @Override
    public WorkspacePath getLocalPath() {
        return new WorkspacePath(getResource().getFullPath().toPortableString());
    }

    @Override
    public WorkspaceFile getFile(WorkspacePath relativePath) {
        return new EclipseWorkspaceFile(getResource().getFile(relativePath.asPortableString()), getIgnoredFileNames());
    }
    
    @Override
    public WorkspaceDirectory getDirectory(WorkspacePath relativePath) {
        return new EclipseWorkspaceDirectory(getResource().getFolder(relativePath.asPortableString()), getIgnoredFileNames());
    }

    @Override
    public List<WorkspaceResource> getChildren() {
        try {
            return Arrays.stream(getResource().members())
                .map(EclipseResources::create)
                .collect(Collectors.toList());
        } catch ( CoreException e ) {
            // TODO - proper exception handling
            throw new RuntimeException(e);
        }
    }
}

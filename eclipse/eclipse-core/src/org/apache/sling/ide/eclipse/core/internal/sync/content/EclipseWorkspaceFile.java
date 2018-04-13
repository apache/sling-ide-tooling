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

import java.io.InputStream;
import java.util.Set;

import org.apache.sling.ide.sync.content.WorkspaceDirectory;
import org.apache.sling.ide.sync.content.WorkspaceFile;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.CoreException;

/**
 * A {@link WorkspaceFile} implemenation based on Eclipse APIs.
 *
 */
public class EclipseWorkspaceFile extends EclipseWorkspaceResource implements WorkspaceFile {

    public EclipseWorkspaceFile(IFile resource, Set<String> ignoredFileNames) {
        super(resource, ignoredFileNames);
    }

    @Override
    public InputStream getContents() {
        try {
            return getResource().getContents();
        } catch (CoreException e) {
            // TODO Auto-generated catch block
            throw new RuntimeException(e);
        }
    }

    @Override
    public WorkspaceDirectory getParent() {
        IContainer parent = getResource().getParent();
        
        if ( parent instanceof IFolder ) 
            return new EclipseWorkspaceDirectory((IFolder) parent, getIgnoredFileNames());
        
        return null;
    }
    @Override
    protected IFile getResource() {
        return (IFile) super.getResource();
    }
}

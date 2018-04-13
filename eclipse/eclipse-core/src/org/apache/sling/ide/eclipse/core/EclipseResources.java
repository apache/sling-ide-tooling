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
package org.apache.sling.ide.eclipse.core;

import java.util.Set;

import org.apache.sling.ide.eclipse.core.internal.Activator;
import org.apache.sling.ide.eclipse.core.internal.sync.content.EclipseWorkspaceDirectory;
import org.apache.sling.ide.eclipse.core.internal.sync.content.EclipseWorkspaceFile;
import org.apache.sling.ide.eclipse.core.internal.sync.content.EclipseWorkspaceProject;
import org.apache.sling.ide.sync.content.WorkspaceResource;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;

public abstract class EclipseResources {

    public static WorkspaceResource create(IResource resource) {
        
        Set<String> ignoredFileNames = Activator.getDefault().getPreferences().getIgnoredFileNamesForSync();
        
        switch ( resource.getType() ) {
        case IResource.FILE:
            return new EclipseWorkspaceFile((IFile) resource, ignoredFileNames);
        case IResource.FOLDER:
            return new EclipseWorkspaceDirectory((IFolder) resource, ignoredFileNames);
        case IResource.PROJECT:
            return new EclipseWorkspaceProject((IProject) resource, ignoredFileNames);
            default:
                throw new IllegalArgumentException("Unable to create a local resource for Eclipse IResource.getType() = " + resource.getType() );
        }
    }
    
    private EclipseResources() {
        
    }
}

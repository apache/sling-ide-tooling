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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Set;

import org.apache.sling.ide.eclipse.core.internal.Activator;
import org.apache.sling.ide.sync.content.WorkspacePath;
import org.apache.sling.ide.sync.content.WorkspaceProject;
import org.apache.sling.ide.sync.content.WorkspaceResource;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.QualifiedName;

public abstract class EclipseWorkspaceResource implements WorkspaceResource {

    private final IResource resource;
    private final Set<String> ignoredFileNames;
    
    protected EclipseWorkspaceResource(IResource resource, Set<String> ignoredFileNames) {
        this.resource = resource;
        this.ignoredFileNames = ignoredFileNames;
    }

    @Override
    public boolean exists() {
        return resource.exists();
    }

    @Override
    public boolean isIgnored() {
        return ignoredFileNames.contains(getName()) ||
                resource.isTeamPrivateMember(IResource.CHECK_ANCESTORS);
    }

    @Override
    public WorkspacePath getLocalPath() {
        return new WorkspacePath(resource.getFullPath().toPortableString());
    }

    @Override
    public Path getOSPath() {
        return Paths.get(resource.getLocation().toOSString());
    }

    @Override
    public WorkspaceProject getProject() {
        return new EclipseWorkspaceProject(resource.getProject(), ignoredFileNames);
    }
    
    @Override
    public long getLastModified() {
        return resource.getModificationStamp();
    }
    
    @Override
    public Object getTransientProperty(String propertyName) {
        try {
            return resource.getSessionProperty(new QualifiedName(Activator.PLUGIN_ID, propertyName));
        } catch (CoreException e) {
            // TODO Auto-generated catch block
            throw new RuntimeException();
        }
    }
    
    protected IResource getResource() {
        return resource;
    }
    
    protected Set<String> getIgnoredFileNames() {
        return ignoredFileNames;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((resource == null) ? 0 : resource.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if ( !(obj instanceof EclipseWorkspaceResource) ) {
            return false;
        }
        
        EclipseWorkspaceResource other = (EclipseWorkspaceResource) obj;
        
        return Objects.equals(this.resource, other.resource);

    }
    
    @Override
    public String toString() {
        return resource.toString();
    }
    
}

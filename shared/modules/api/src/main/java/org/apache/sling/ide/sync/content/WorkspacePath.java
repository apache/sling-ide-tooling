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

import java.nio.file.Path;
import java.util.Objects;

import org.apache.sling.ide.util.PathUtil;

/**
 * A path in the local workspace
 * 
 * <p>The workspace path always uses the forward slash ( <tt>/</tt> ) for separating segments.</p>
 *
 */
public class WorkspacePath {

    private final String portablePath;
    
    public WorkspacePath(String portablePath) {
        if ( portablePath == null )
            throw new IllegalArgumentException("Invalid path :'" + portablePath+"'");
        
        if ( portablePath.length() > 1 && portablePath.endsWith("/") )
            portablePath = portablePath.substring(0, portablePath.length() - 1);
        
        this.portablePath = portablePath;
    }
    
    public String getName() {
        return PathUtil.getName(portablePath);
    }
    
    public String asPortableString() {
        return portablePath;
    }
    
    /**
     * Creates relative bath between this path and the one passed as an argument.
     * 
     * <p>This method uses the same name uses the same meaning for the parameters` as the {@link Path#relativize(Path)} method.</p>
     * 
     * <p>For this comparison the paths are made absolute, but the returned path is relative.</p>
     * 
     * @param other the potential parent path
     * @return a relative path, or <code>null</code> is the paths are unrelated
     */
    public WorkspacePath relativize(WorkspacePath other) {

        String ours = absolute().portablePath;
        String theirs = other.absolute().portablePath;
        
        if ( ours.equals(theirs) )
            return new WorkspacePath("");
        
        if ( theirs.startsWith(ours) )
            return new WorkspacePath(theirs.substring(ours.length() + 1));
        
        return null;
    }

    public boolean isRoot() {
        return portablePath.equals("/");
    }
    
    public WorkspacePath getParent() {
        String path = PathUtil.getParent(portablePath);
        if ( path == null )
            return null;
        return new WorkspacePath(path);
    }
    
    public WorkspacePath absolute() {
        if ( isAbsolute() )
            return this;
        
        return new WorkspacePath('/' + portablePath);
    }

    private boolean isAbsolute() {
        return portablePath.charAt(0) == '/';
    }
    
    public WorkspacePath append(String name) {
        if ( name == null || name.isEmpty() || name.indexOf('/') != -1)
            throw new IllegalArgumentException("Invalid name: '" + name + "'");
        
        return new WorkspacePath(portablePath + '/' + name);
        
    }
    
    public WorkspacePath append(WorkspacePath other) {
        
        if ( other == null )
            throw new IllegalArgumentException("Unable to append null path");
        
        return new WorkspacePath(PathUtil.join(portablePath, other.portablePath));
    }

    @Override
    public boolean equals(Object obj) {
        if ( !(obj instanceof WorkspacePath) )
            return false;
        
        return Objects.equals(portablePath, ((WorkspacePath) obj).portablePath);
    }
    
    @Override
    public String toString() {
        return asPortableString();
    }
}

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
package org.apache.sling.ide.transport;

import java.util.Objects;

import org.apache.sling.ide.util.PathUtil;

/**
 * A path in the remote repository
 * 
 * <p>The repository path always uses the forward slash ( <tt>/</tt> ) for separating segments.</p>
 * 
 * @see {@link PathUtil}
 */
public class RepositoryPath {
    
    private final String path;

    public RepositoryPath(String path) {
        // validate it is not null or empty and starts with a slash
        if (path == null || path.isEmpty() || !path.startsWith("/"))
            throw new IllegalArgumentException("Invalid repository path: " + path);
        
        // TODO - more validations
        this.path = path;
    }
    
    public boolean isParent(RepositoryPath maybeChild) {
        return PathUtil.isParent(asString(), maybeChild.asString());
    }

    public boolean isAncestor(RepositoryPath other) {
        return PathUtil.isAncestor(asString(), other.asString());
    }    
    
    public boolean isDescendent(RepositoryPath other) {
        return PathUtil.isDescendent(asString(), other.asString());
    }
    
    public String getName() {
        return PathUtil.getName(path);
    }
    
    public RepositoryPath getParent() {
        return new RepositoryPath(PathUtil.getParent(asString()));
    }
    
    public RepositoryPath addChild(String name) {
        return new RepositoryPath(PathUtil.join(path, name));
    }
    
    public String asString() {
        return path;
    }

    @Override
    public int hashCode() {
        return Objects.hash(path);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        RepositoryPath other = (RepositoryPath) obj;
        return Objects.equals(path, other.path);
    }

    @Override
    public String toString() {
        return "RepositoryPath [path=" + path + "]";
    }
}

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
package org.apache.sling.ide.util;

import org.apache.sling.ide.serialization.SerializationManager;
import org.apache.sling.ide.sync.content.WorkspacePath;
import org.apache.sling.ide.transport.RepositoryPath;

/**
 * Utility class that provides implementations useful for both {@link WorkspacePath} and {@link RepositoryPath}
 * 
 * <p><strong>NOTE:</strong> The two path implementations are intentionally kept separate to avoid confusion about
 * which kind of path is being used. Conversions must be done via {@link SerializationManager#getRepositoryPath(WorkspacePath)}
 * and not fall back on the String form of the paths.</p>
 */
public class PathUtil {

    public static String join(String first, String second) {

        boolean repoUrlHasTrailingSlash = first.endsWith("/");
        boolean relativePathHasLeadingSlash = !second.isEmpty() && second.charAt(0) == '/';

        if (repoUrlHasTrailingSlash ^ relativePathHasLeadingSlash)
            return first + second;
        if (!repoUrlHasTrailingSlash && !relativePathHasLeadingSlash)
            return first + '/' + second;
        if (repoUrlHasTrailingSlash && relativePathHasLeadingSlash)
            return first + second.substring(1);

        throw new AssertionError("unreachable");
    }

    public static String getName(String path) {
        
        if ( path.length() == 1 && path.charAt(0) == '/')
            return path;

        return path.substring(path.lastIndexOf('/') + 1);
    }

    public static String getParent(String path) {

        if (path == null || path.length() == 0 || path.charAt(0) != '/' || path.indexOf('/') == -1) {
            throw new IllegalArgumentException("No a valid or absolut path: " + path);
        }

        if (path.equals("/")) {
            return null;
        }
        

        if (path.lastIndexOf('/') == 0) {
            return "/";
        }

        return path.substring(0, path.lastIndexOf('/'));

    }
    
    public static boolean isAncestor(String ancestor, String child) {
        
        while ( (child = getParent(child)) != null ) {
            if ( child.equals(ancestor)) {
                return true;
            }
        }
        
        return false;
    }
    
    public static boolean isParent(String parentPath, String childPath) {

        if (!isDescendent(parentPath, childPath)) {
            return false;
        }

        for (int i = parentPath.length() + 1; i < childPath.length(); i++) {
            if (childPath.charAt(i) == '/') {
                return false;
            }
        }

        return true;
    }
    
    public static boolean isDescendent(String parentPath, String childPath) {
        
        if (parentPath.equals("/")) {
            return childPath.length() > 1;
        }

        return parentPath.length() < childPath.length() && childPath.charAt(parentPath.length()) == '/'
                    && childPath.startsWith(parentPath);
    }
    
    private PathUtil() {
        
    }
}

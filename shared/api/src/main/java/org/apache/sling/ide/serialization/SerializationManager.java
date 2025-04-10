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
package org.apache.sling.ide.serialization;

import java.io.File;
import java.io.IOException;

import org.apache.sling.ide.sync.content.WorkspaceFile;
import org.apache.sling.ide.sync.content.WorkspacePath;
import org.apache.sling.ide.sync.content.WorkspaceResource;
import org.apache.sling.ide.transport.Repository;
import org.apache.sling.ide.transport.ResourceProxy;

public interface SerializationManager {

    /**
     * Checks if the given file is a serialization file.
     * 
     * <p>May load the file to check its contents.</p>
     * 
     * @param file the workspace file
     * @return true if the file is a serialization file
     */
    boolean isSerializationFile(WorkspaceFile file);

    /**
     * @param serializationFilePath the full OS path to the serialization file
     * @return
     */
    String getBaseResourcePath(String serializationFilePath);

    /**
     * Returns the serialization file for the given resource and serialization kind.
     * 
     * @param baseResource the resource to get the serialisation file for
     * @param serializationKind the kind of serialization
     * @return the serialization file for the given resource. Never null, may not exist.
     */
    WorkspaceFile getSerializationFile(WorkspaceResource baseResource, SerializationKind serializationKind);

    /**
     * Returns the (remote) repository path for the given local path.
     * 
     * @param localPath the local path
     * @return the repository path
     */
    String getRepositoryPath(WorkspacePath localPath);

    String getOsPath(String repositoryPath);

    SerializationDataBuilder newBuilder(Repository repository, File contentSyncRoot) throws SerializationException;

    /**
     * Reads the serialization data from the given file and creates a resource proxy
     * 
     * @param serializationFile the file to read
     * @return the resource proxy
     * @throws IOException
     */
    ResourceProxy readSerializationData(WorkspaceFile serializationFile) throws IOException;
    
    void destroy();
}

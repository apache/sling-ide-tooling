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
import java.io.InputStream;

import org.apache.sling.ide.sync.content.WorkspaceFile;
import org.apache.sling.ide.sync.content.WorkspaceResource;
import org.apache.sling.ide.transport.Repository;
import org.apache.sling.ide.transport.ResourceProxy;

public interface SerializationManager {

    void destroy();

    /**
     * @param filePath the filesystem path
     * @return
     */
    boolean isSerializationFile(WorkspaceFile file);

    /**
     * @param serializationFilePath the full OS path to the serialization file
     * @return
     */
    String getBaseResourcePath(String serializationFilePath);

    /**
     * @param baseResource the resource to get the serialisation path for
     * @param serializationKind
     * @return
     */
    WorkspaceFile getSerializationFilePath(WorkspaceResource baseResource, SerializationKind serializationKind);

    String getRepositoryPath(String osPath);

    String getOsPath(String repositoryPath);

    SerializationDataBuilder newBuilder(Repository repository, File contentSyncRoot) throws SerializationException;

    /**
     * @param filePath The filePath, in repository format
     * @param source
     * @return
     * @throws IOException
     */
    ResourceProxy readSerializationData(String filePath, InputStream source) throws IOException;
}

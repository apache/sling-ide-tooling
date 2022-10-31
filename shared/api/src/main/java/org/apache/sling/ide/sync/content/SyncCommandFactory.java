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

import java.io.IOException;

import org.apache.sling.ide.transport.Command;
import org.apache.sling.ide.transport.Repository;
import org.apache.sling.ide.transport.ResourceAndInfo;

/**
 * Creates commands in response to local resource changes 
 */
public interface SyncCommandFactory {
    
    String PN_IMPORT_MODIFICATION_TIMESTAMP = "importModificationTimestamp";
    
    /**
     * Creates a command in response to a local resource being deleted
     * 
     * @param repository the repository that will create the command
     * @param resource the resource that was deleted 
     * @return the commmand to execute
     * @throws IOException I/O related problems
     */
    Command<?> newCommandForRemovedResource(Repository repository, WorkspaceResource resource) throws IOException;

    /**
     * Creates a command in response to a local resource being added or updated
     * 
     * @param repository the repository that will create the command
     * @param resource the resource that was added or updated
     * @return the commmand to execute
     * @throws IOException I/O related problems
     */
    Command<?> newCommandForAddedOrUpdatedResource(Repository repository, WorkspaceResource resource) throws IOException;
    
    /**
     * Creates a command which reorders the child nodes of the node corresponding to the specified local resource 
     * 
     * @param repository the repository that will create the command
     * @param resource the resource whose children should be reorderer
     * @return the command to execut
     * @throws IOException I/O related problems
     */
    Command<Void> newReorderChildNodesCommand(Repository repository, WorkspaceResource resource) throws IOException;
    
    /**
     * Convenience method which builds a <tt>ResourceAndInfo</tt> info for a specific <tt>IResource</tt>
     * 
     * @param resource the resource to process
     * @param repository the repository, used to extract serialization information for different resource types
     * @return the built object, or null if one could not be built
     * @throws IOException I/O related problems
     */
    ResourceAndInfo buildResourceAndInfo(WorkspaceResource resource, Repository repository) throws IOException;
}

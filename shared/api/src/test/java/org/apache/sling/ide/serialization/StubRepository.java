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

import org.apache.sling.ide.sync.content.WorkspaceFile;
import org.apache.sling.ide.transport.Command;
import org.apache.sling.ide.transport.CommandContext;
import org.apache.sling.ide.transport.FallbackNodeTypeRegistry;
import org.apache.sling.ide.transport.NodeTypeRegistry;
import org.apache.sling.ide.transport.Repository;
import org.apache.sling.ide.transport.RepositoryInfo;
import org.apache.sling.ide.transport.RepositoryPath;
import org.apache.sling.ide.transport.ResourceProxy;

public class StubRepository implements Repository {

    @Override
    public Command<ResourceProxy> newListChildrenNodeCommand(final RepositoryPath path) {
        return null;
    }

    @Override
    public Command<ResourceProxy> newGetNodeContentCommand(RepositoryPath path) {
        return null;
    }

    @Override
    public Command<byte[]> newGetNodeCommand(RepositoryPath path) {
        return null;
    }

    @Override
    public Command<Void> newDeleteNodeCommand(RepositoryPath path) {
        return null;
    }

    @Override
    public Command<Void> newAddOrUpdateNodeCommand(CommandContext context, WorkspaceFile fileInfo, ResourceProxy resourceInfo,
            CommandExecutionFlag... flags) {
        return null;
    }

    @Override
    public Command<Void> newReorderChildNodesCommand(ResourceProxy resourceProxy) {
        return null;
    }

    @Override
    public RepositoryInfo getRepositoryInfo() {
        return null;
    }
    
    @Override
    public NodeTypeRegistry getNodeTypeRegistry() {
        
        return FallbackNodeTypeRegistry.createRegistryWithDefaultNodeTypes();
    }
}
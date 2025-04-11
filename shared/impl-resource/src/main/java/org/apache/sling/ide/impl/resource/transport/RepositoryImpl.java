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
package org.apache.sling.ide.impl.resource.transport;

import org.apache.commons.httpclient.HttpClient;
import org.apache.sling.ide.log.Logger;
import org.apache.sling.ide.sync.content.WorkspaceFile;
import org.apache.sling.ide.transport.Command;
import org.apache.sling.ide.transport.CommandContext;
import org.apache.sling.ide.transport.NodeTypeRegistry;
import org.apache.sling.ide.transport.Repository;
import org.apache.sling.ide.transport.RepositoryInfo;
import org.apache.sling.ide.transport.ResourceProxy;
import org.apache.sling.ide.transport.Result;

public class RepositoryImpl implements Repository {
	
    private final HttpClient httpClient = new HttpClient();
    private final RepositoryInfo repositoryInfo;
    private final Logger logger;

    public RepositoryImpl(RepositoryInfo repositoryInfo, Logger logger) {
        this.repositoryInfo = repositoryInfo;
        this.logger = logger;
    }

	@Override
    public Command<Void> newDeleteNodeCommand(final String path) {
        return new DeleteNodeCommand(path, repositoryInfo, httpClient);
	}
	
	@Override
    public Command<ResourceProxy> newListChildrenNodeCommand(final String path) {
        return new ListChildrenCommand(repositoryInfo, httpClient, path + ".1.json");
	}

	@Override
	public Command<byte[]> newGetNodeCommand(final String path) {
        return new GetNodeCommand(repositoryInfo, httpClient, path);
	}
	
	@Override
    public Command<ResourceProxy> newGetNodeContentCommand(final String path) {
        return new GetNodeContentCommand(repositoryInfo, httpClient, path + ".json");
	}
	
	@Override
    public Command<Void> newAddOrUpdateNodeCommand(CommandContext context, final WorkspaceFile fileInfo, ResourceProxy resource,
            CommandExecutionFlag... flags) {
        if (flags.length != 0) {
            throw new UnsupportedOperationException("This implementation does not support any flags");
        }
		
        return new UpdateContentCommand(repositoryInfo, httpClient, fileInfo,
                resource.getProperties());
	}

    @Override
    public Command<Void> newReorderChildNodesCommand(ResourceProxy resourceProxy) {
        return new AbstractCommand<>(repositoryInfo, httpClient, resourceProxy.getPath()) {
            @Override
            public Result<Void> execute() {
                // TODO - this is a no-op
                return null;
            }
        };
    }

    @Override
    public RepositoryInfo getRepositoryInfo() {
        return repositoryInfo;
    }
    
    @Override
    public NodeTypeRegistry getNodeTypeRegistry() {
        throw new IllegalStateException("not yet implemented");
    }
}

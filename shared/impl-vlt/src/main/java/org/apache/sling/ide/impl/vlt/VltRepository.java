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
package org.apache.sling.ide.impl.vlt;

import javax.jcr.Credentials;
import javax.jcr.RepositoryException;

import org.apache.sling.ide.jcr.RepositoryUtils;
import org.apache.sling.ide.log.Logger;
import org.apache.sling.ide.sync.content.WorkspaceFile;
import org.apache.sling.ide.transport.Command;
import org.apache.sling.ide.transport.CommandContext;
import org.apache.sling.ide.transport.NodeTypeRegistry;
import org.apache.sling.ide.transport.Repository;
import org.apache.sling.ide.transport.RepositoryInfo;
import org.apache.sling.ide.transport.RepositoryPath;
import org.apache.sling.ide.transport.ResourceProxy;

/**
 * The <tt>VltRepository</tt> is a Repository implementation backed by <tt>FileVault</tt>
 * 
 */
public class VltRepository implements Repository {

    private final RepositoryInfo repositoryInfo;
    private NodeTypeRegistry ntRegistry;

    private javax.jcr.Repository jcrRepo;
    private Credentials credentials;
    private boolean disconnected = false;
    private final Logger logger;

    public VltRepository(RepositoryInfo repositoryInfo, Logger logger) {
        this.repositoryInfo = repositoryInfo;
        this.logger = logger;
    }

    public synchronized void disconnected() {
        this.disconnected = true;
    }

    public synchronized boolean isDisconnected() {
        return disconnected;
    }

    @Override
    public RepositoryInfo getRepositoryInfo() {
        return repositoryInfo;
    }

    public void connect() {
        try {
            jcrRepo = RepositoryUtils.getRepository(repositoryInfo);
            credentials = RepositoryUtils.getCredentials(repositoryInfo);

        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
        // loading nodeTypeRegistry:
        loadNodeTypeRegistry();
    }

    private void loadNodeTypeRegistry() {
        try {
            ntRegistry = new VltNodeTypeRegistry(this);
        } catch (org.apache.sling.ide.transport.RepositoryException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Command<Void> newAddOrUpdateNodeCommand(CommandContext context, WorkspaceFile fileInfo, ResourceProxy resource,
            CommandExecutionFlag... flags) {
        return new AddOrUpdateNodeCommand(jcrRepo, credentials, context, fileInfo, resource, logger, flags);
    }

    @Override
    public Command<Void> newReorderChildNodesCommand(ResourceProxy resource) {
        return new ReorderChildNodesCommand(jcrRepo, credentials, resource, logger);
    }

    @Override
    public Command<Void> newDeleteNodeCommand(RepositoryPath path) {
        return new DeleteNodeCommand(jcrRepo, credentials, path, logger);
    }

    @Override
    public Command<ResourceProxy> newListChildrenNodeCommand(RepositoryPath path) {
        return new ListChildrenCommand(jcrRepo, credentials, path, logger);
    }

    @Override
    public Command<ResourceProxy> newGetNodeContentCommand(RepositoryPath path) {
        return new GetNodeContentCommand(jcrRepo, credentials, path, logger);
    }

    @Override
    public Command<byte[]> newGetNodeCommand(RepositoryPath path) {
    	return new GetNodeCommand(jcrRepo, credentials, path, logger);
    }

    Command<ResourceProxy> newListTreeNodeCommand(RepositoryPath path, int levels) {
        return new ListTreeCommand(jcrRepo, credentials, path, levels, logger);
    }
    
    @Override
    public synchronized NodeTypeRegistry getNodeTypeRegistry() {
        if (repositoryInfo==null) {
            throw new IllegalStateException("repositoryInfo must not be null");
        }
        return ntRegistry;
    }

}

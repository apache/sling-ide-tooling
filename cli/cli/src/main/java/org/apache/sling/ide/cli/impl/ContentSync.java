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
package org.apache.sling.ide.cli.impl;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;

import org.apache.sling.ide.cli.impl.DirWatcher.Event;
import org.apache.sling.ide.content.sync.fs.FSResources;
import org.apache.sling.ide.filter.FilterLocator;
import org.apache.sling.ide.log.Logger;
import org.apache.sling.ide.sync.content.SyncCommandFactory;
import org.apache.sling.ide.sync.content.WorkspacePath;
import org.apache.sling.ide.sync.content.WorkspacePaths;
import org.apache.sling.ide.sync.content.WorkspaceProject;
import org.apache.sling.ide.transport.Command;
import org.apache.sling.ide.transport.Repository;
import org.apache.sling.ide.transport.RepositoryFactory;
import org.apache.sling.ide.transport.RepositoryInfo;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(immediate=true)
public class ContentSync {
    
    @Reference
    private Logger logger;
    
    @Reference
    private RepositoryFactory repoFactory;
    
    @Reference
    private SyncCommandFactory commandFactory;
    
    @Reference
    private FilterLocator filterLocator;
    
    private DirWatcher watcher;
    
    private Thread watcherThread;
    

    protected void activate() throws Exception {

        File projectDir = new File("/home/robert/Documents/workspace/content003");
        
        WorkspaceProject prj = FSResources.create(projectDir, projectDir, filterLocator);
        
        logger.trace("Working on project {0} at {1}", prj.getName(), prj.getOSPath());
        
        Repository repo = repoFactory.connectRepository(new RepositoryInfo("admin", "admin", "http://localhost:8080"));
        
        repo.newListChildrenNodeCommand("/").execute();
        
        logger.trace("Connected to {0} ", repo.getRepositoryInfo());
        
        Path syncDirPath = prj.getSyncDirectory().getOSPath();
        
        watcher = new DirWatcher(syncDirPath);
        
        logger.trace("Watching syncDir {0}", syncDirPath);
        
        watcherThread = new Thread(new Runnable()  {
            @Override
            public void run() {
                try {
                    while ( ! Thread.currentThread().isInterrupted() ) {

                        Event event = watcher.poll();
            
                        Path path = event.getPath();
                        
                        WorkspacePath resourceRelativePath = WorkspacePaths.fromOsPath(path);
                        logger.trace("Change detected in workspace path {0}", resourceRelativePath);
                        if ( event.getKind() == StandardWatchEventKinds.ENTRY_CREATE || 
                                event.getKind() == StandardWatchEventKinds.ENTRY_MODIFY ) {
                            try {
                                Command<?> cmd = commandFactory.
                                    newCommandForAddedOrUpdatedResource(repo, prj.getSyncDirectory().getFile(resourceRelativePath));
                                if ( cmd != null )
                                    cmd.execute();
                            } catch (IOException e) {
                                logger.warn("Sync failed for path " + resourceRelativePath , e);
                            }
                        }
                        
                        if ( event.getKind() == StandardWatchEventKinds.ENTRY_DELETE ) {
                            try {
                                Command<?> cmd = commandFactory.newCommandForRemovedResource(repo, prj.getSyncDirectory().getFile(resourceRelativePath));
                                if ( cmd != null )
                                    cmd.execute();
                            } catch (IOException e) {
                                logger.warn("Sync failed for path " + resourceRelativePath , e);
                            }
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
        
        watcherThread.start();
    }
    
    protected void deactivate() throws Exception {
        
        if ( watcher != null ) {
            watcherThread.interrupt();
        }
    }
}

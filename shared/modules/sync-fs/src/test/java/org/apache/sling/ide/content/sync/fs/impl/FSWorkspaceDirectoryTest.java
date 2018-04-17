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
package org.apache.sling.ide.content.sync.fs.impl;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.sling.ide.sync.content.WorkspaceDirectory;
import org.apache.sling.ide.sync.content.WorkspaceFile;
import org.apache.sling.ide.sync.content.WorkspacePath;
import org.apache.sling.ide.sync.content.WorkspaceResource;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class FSWorkspaceDirectoryTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();
    private File projectDir;
    private File syncRoot;
    private FSWorkspaceProject project;
    private File fileChild;
    private File dirChild;
    private WorkspaceDirectory dir; 
    
    @Before
    public void prepareProject() throws IOException {
        
        projectDir = folder.newFolder("test-project");
        syncRoot = new File(projectDir, "jcr_root");
        syncRoot.mkdir();
        
        project = new FSWorkspaceProject(projectDir, syncRoot, new MockFilterLocator());
        fileChild = new File(syncRoot, "file.txt");
        fileChild.createNewFile();
        
        dirChild = new File(syncRoot, "dir");
        dirChild.mkdir();
        
        dir = project.getDirectory(new WorkspacePath("jcr_root"));
    }
    
    @Test
    public void basicOperations() {
        
        assertThat("dir.exists", dir.exists(), equalTo(true));
        assertThat("dir.osPath", dir.getOSPath(), equalTo(syncRoot.toPath()));
        assertThat("dir.localPath", dir.getLocalPath(), equalTo(new WorkspacePath("/test-project/jcr_root")));
        assertThat("dir.lastModified", dir.getLastModified(), equalTo(syncRoot.lastModified()));
        assertThat("dir.name", dir.getName(), equalTo("jcr_root"));
        assertThat("dir.project", dir.getProject(), sameInstance(project));
    }
    
    @Test
    public void getChildren() {
        
        List<WorkspaceResource> children = dir.getChildren();
        assertThat("children.size", children, hasSize(2));
        List<WorkspaceFile> files = children.stream()
                .filter( r -> (r instanceof WorkspaceFile ) )
                .map( r -> ( (WorkspaceFile) r))
                .collect(Collectors.toList());
        List<WorkspaceDirectory> dirs = children.stream()
                .filter( r -> (r instanceof WorkspaceDirectory ) )
                .map( r -> ( (WorkspaceDirectory) r))
                .collect(Collectors.toList());
        
        assertThat("files.size", files, hasSize(1));
        assertThat("dirs.size", dirs, hasSize(1));
        
        WorkspaceFile file = files.get(0);
        assertThat("file.exists", file.exists(), equalTo(true));
        assertThat("file.localPath", file.getLocalPath(), equalTo(new WorkspacePath("/test-project/jcr_root/file.txt")));
        assertThat("file.osPath", file.getOSPath(), equalTo(fileChild.toPath()));
        
        WorkspaceDirectory dir = dirs.get(0);
        assertThat("dir.exists", dir.exists(), equalTo(true));
        assertThat("dir.localPath", dir.getLocalPath(), equalTo(new WorkspacePath("/test-project/jcr_root/dir")));
        assertThat("dir.osPath", dir.getOSPath(), equalTo(dirChild.toPath()));
    }
    
    @Test
    public void getFile_exists() {
        WorkspaceFile file = dir.getFile(new WorkspacePath("file.txt"));
        assertThat("file.exists", file.exists(), equalTo(true));
        assertThat("file.localPath", file.getLocalPath(), equalTo(new WorkspacePath("/test-project/jcr_root/file.txt")));
    }

    @Test
    public void getFile_doesNotExist() {
        WorkspaceFile file = dir.getFile(new WorkspacePath("does-not-exist.txt"));
        assertThat("file.exists", file.exists(), equalTo(false));
        assertThat("file.localPath", file.getLocalPath(), equalTo(new WorkspacePath("/test-project/jcr_root/does-not-exist.txt")));
    }
    
    @Test
    public void getFile_isDirectory() {
        WorkspaceFile file = dir.getFile(new WorkspacePath("dir"));
        assertThat("file.exists", file.exists(), equalTo(false));
        assertThat("file.localPath", file.getLocalPath(), equalTo(new WorkspacePath("/test-project/jcr_root/dir")));
    }
    
    @Test
    public void getDir_exists() {
        WorkspaceDirectory directory = dir.getDirectory(new WorkspacePath("dir"));
        assertThat("directory.exists", directory.exists(), equalTo(true));
        assertThat("directory.localPath", directory.getLocalPath(), equalTo(new WorkspacePath("/test-project/jcr_root/dir")));
    }
    
    @Test
    public void getDir_doesNotExist() {
        WorkspaceDirectory directory = dir.getDirectory(new WorkspacePath("does-not-exist"));
        assertThat("directory.exists", directory.exists(), equalTo(false));
        assertThat("file.localPath", directory.getLocalPath(), equalTo(new WorkspacePath("/test-project/jcr_root/does-not-exist")));
    }
    
    @Test
    public void getDir_isFile() {
        WorkspaceDirectory directory = dir.getDirectory(new WorkspacePath("file.txt"));
        assertThat("directory.exists", directory.exists(), equalTo(false));
        assertThat("file.localPath", directory.getLocalPath(), equalTo(new WorkspacePath("/test-project/jcr_root/file.txt")));
    }
}

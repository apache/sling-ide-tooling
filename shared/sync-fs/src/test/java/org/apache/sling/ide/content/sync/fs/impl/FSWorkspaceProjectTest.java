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
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.IOException;

import org.apache.sling.ide.sync.content.WorkspaceDirectory;
import org.apache.sling.ide.sync.content.WorkspacePath;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class FSWorkspaceProjectTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();
    private File projectDir;
    private File syncRoot;
    private File metaInfDir;
    private File vaultDir;
    private File jcrRoot;
    private FSWorkspaceProject project; 
    
    @Before
    public void prepareProject() throws IOException {
        projectDir = folder.newFolder("test-project");
        syncRoot = new File(projectDir, "content");
        syncRoot.mkdir();
        
        metaInfDir = new File(syncRoot, "META-INF");
        metaInfDir.mkdir();
        vaultDir = new File(metaInfDir, "vault");
        vaultDir.mkdir();
        File filterXml = new File(vaultDir, "filter.xml");
        filterXml.createNewFile();
        
        jcrRoot = new File(syncRoot, "jcr_root");
        jcrRoot.mkdir();
        
        project = new FSWorkspaceProject(projectDir, syncRoot, new MockFilterLocator());
    }
    
    @Test
    public void basicOperations() {

        assertThat("project.exists", project.exists(), equalTo(true));
        assertThat("project.name", project.getName(), equalTo("test-project"));
        assertThat("project.localPath", project.getLocalPath(), equalTo(new WorkspacePath("/test-project")));
        assertThat("project.osPath", project.getOSPath(), equalTo(projectDir.toPath()));
        assertThat("project.project", project.getProject(), sameInstance(project));
        assertThat("project.lastModified", project.getLastModified(), equalTo(projectDir.lastModified()));
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void invalidDir_null() {
        new FSWorkspaceProject(null, null, null);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void invalidDir_file() throws IOException {
        new FSWorkspaceProject(folder.newFile(), null, null);
    }

    @Test
    public void accessChildResources() throws IOException {
        
        WorkspaceDirectory directory = project.getDirectory(new WorkspacePath("content"));
        assertThat("directory", directory, not(nullValue()));
        assertThat("directory.exists", directory.exists(), equalTo(true));
        assertThat("directory.osPath", directory.getOSPath(), equalTo(syncRoot.toPath()));
        assertThat("directory.localPath", directory.getLocalPath(), equalTo(new WorkspacePath("/test-project/content")));
    }
    
    @Test
    public void syncDirLocation() throws IOException {
        
        WorkspaceDirectory syncDir = project.getSyncDirectory();
        assertThat("syncDir.exists", syncDir.exists(), equalTo(true));
        assertThat("syncDir.localPath", syncDir.getLocalPath(), equalTo(new WorkspacePath("/test-project/content/jcr_root")));
    }
    
    @Test
    public void filter() throws IOException {

        assertThat("filter", project.getFilter(), equalTo(MockFilterLocator.MOCK_FILTER));
    }
    
    @Test
    public void getDirectory_missing() {
        
        WorkspaceDirectory directory = project.getDirectory(new WorkspacePath("missing"));
        assertThat("directory.exists", directory.exists(), equalTo(false));
        assertThat("directory.localPath", directory.getLocalPath(), equalTo(new WorkspacePath("/test-project/missing")));
    }
    
    @Test
    public void getDirectory_file() {
        
        WorkspaceDirectory directory = project.getDirectory(new WorkspacePath("content/META-INF/vault/filter.xml"));
        assertThat("directory.exists", directory.exists(), equalTo(false));
        assertThat("directory.localPath", directory.getLocalPath(), equalTo(new WorkspacePath("/test-project/content/META-INF/vault/filter.xml")));
    }   
}

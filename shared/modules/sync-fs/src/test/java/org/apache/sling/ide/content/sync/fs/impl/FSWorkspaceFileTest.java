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

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.apache.sling.ide.sync.content.WorkspaceDirectory;
import org.apache.sling.ide.sync.content.WorkspaceFile;
import org.apache.sling.ide.sync.content.WorkspacePath;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class FSWorkspaceFileTest {

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
        try (FileWriter fw = new FileWriter(fileChild)) {
            fw.write("hello, world");
        }

        dirChild = new File(syncRoot, "dir");
        dirChild.mkdir();

        dir = project.getDirectory(new WorkspacePath("jcr_root"));
    }

    @Test
    public void basicOperations() throws IOException {
     
        WorkspaceFile file = project.getDirectory(new WorkspacePath("jcr_root")).getFile(new WorkspacePath("file.txt"));
        
        assertThat("file.lastModified", file.getLastModified(), equalTo(fileChild.lastModified()));
        assertThat("file.localPath", file.getLocalPath(), equalTo(new WorkspacePath("/test-project/jcr_root/file.txt")));
        assertThat("file.name", file.getName(), equalTo("file.txt"));
        assertThat("file.contents", IOUtils.toString(file.getContents(), UTF_8), equalTo("hello, world"));
        assertThat("file.parent", file.getParent(), equalTo(dir));
    }
}

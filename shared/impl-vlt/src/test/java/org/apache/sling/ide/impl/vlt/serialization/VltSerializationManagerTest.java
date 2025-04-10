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
package org.apache.sling.ide.impl.vlt.serialization;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.apache.sling.ide.impl.vlt.Slf4jLogger;
import org.apache.sling.ide.sync.content.WorkspacePath;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class VltSerializationManagerTest {

    private VltSerializationManager serializationManager;

    @Rule
    public TemporaryFolder trash = new TemporaryFolder();

    @Before
    public void init() {
        serializationManager = new VltSerializationManager(new Slf4jLogger(), null);
    }

    @Test
    public void getOsName_CleanName() {
        assertThat(serializationManager.getLocalName("test"), is("test"));
    }

    @Test
    public void getOsName_MangledName() {
        assertThat(serializationManager.getLocalName("jcr:content"), is("_jcr_content"));
    }
    
    @Test(expected =  IllegalArgumentException.class)
    public void getOsName_Null() {
        serializationManager.getLocalName(null);
    }

    @Test(expected =  IllegalArgumentException.class)
    public void getOsName_Invalid() {
        serializationManager.getLocalName("a/path");
    }

    @Test
    public void getRepositoryPath_CleanName() {
        assertThat(serializationManager.getRepositoryPath(new WorkspacePath("/content/test")), is("/content/test"));
    }

    @Test
    public void getRepositoryPath_MangledName() {
        assertThat(serializationManager.getRepositoryPath(new WorkspacePath("/content/test/_jcr_content")),
                is("/content/test/jcr:content"));
    }

    @Test
    public void getRepositoryPath_SerializationDir() {
        assertThat(serializationManager.getRepositoryPath(new WorkspacePath("/content/test.dir/file")), is("/content/test/file"));
    }

}

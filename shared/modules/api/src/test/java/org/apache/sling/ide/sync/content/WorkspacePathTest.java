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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public class WorkspacePathTest {

    @Test(expected = IllegalArgumentException.class)
    public void nullPath() {
        new WorkspacePath(null);
    }
    

    public void emptyPath() {
        assertThat(new WorkspacePath("").asPortableString(), equalTo(""));
    }
    
    @Test
    public void removeTrailingSlash() {
        assertThat(new WorkspacePath("/first/").asPortableString(), equalTo("/first"));
    }
    
    @Test
    public void getName_root() {
        assertThat(new WorkspacePath("/").getName(), equalTo("/"));
    }
    
    @Test
    public void getName_firstLevelChild() {
        assertThat(new WorkspacePath("/first").getName(), equalTo("first"));
    }
    
    @Test
    public void getName_secondLevelChild() {
        assertThat(new WorkspacePath("/first/second").getName(), equalTo("second"));
    }
    
    @Test
    public void absolute_root() {
        assertThat(new WorkspacePath("/").absolute().asPortableString(), equalTo("/"));
    }
    
    @Test
    public void absolute_noop() {
        assertThat(new WorkspacePath("/first").absolute().asPortableString(), equalTo("/first"));
    }
    
    @Test
    public void absolute_changed() {
        assertThat(new WorkspacePath("first").absolute().asPortableString(), equalTo("/first"));
    }
    
    @Test
    public void makeRelative_parentAndChild() {
        
        WorkspacePath parent = new WorkspacePath("first");
        WorkspacePath child = new WorkspacePath("first/second/third");
        
        assertThat(parent.relativize(child).asPortableString(), equalTo("second/third"));
    }
    
    @Test
    public void makeRelative_same() {
        
        WorkspacePath path = new WorkspacePath("first");
        
        assertThat(path.relativize(path).asPortableString(), equalTo(""));
    }
    
    @Test
    public void makeRelative_unrelated() {
        
        assertThat(new WorkspacePath("/first").relativize(new WorkspacePath("/second")), nullValue());
    }
    
    @Test
    public void isRoot_root() {
        
        assertThat(new WorkspacePath("/").isRoot(), equalTo(true));
    }
    
    @Test
    public void isRoot_notRoot() {
        assertThat(new WorkspacePath("/first").isRoot(), equalTo(false));
    }
    
    @Test
    public void getParent_root() {
        assertThat(new WorkspacePath("/").getParent(), nullValue());
    }
    
    @Test
    public void getParent_firstLevel() {
        assertThat(new WorkspacePath("/first").getParent().asPortableString(), equalTo("/"));
    }
    
    @Test
    public void getParent_secondLevel() {
        assertThat(new WorkspacePath("/first/second").getParent().asPortableString(), equalTo("/first"));
    }
    
    @Test
    public void equals_same() {
        assertThat(new WorkspacePath("/first").equals(new WorkspacePath("/first")), equalTo(true));
    }

    @Test
    public void equals_different() {
        assertThat(new WorkspacePath("/first").equals(new WorkspacePath("/second")), equalTo(false));
    }

    @Test
    public void equals_relativeVsAbsolute() {
        assertThat(new WorkspacePath("/first").equals(new WorkspacePath("first")), equalTo(false));
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void append_notRelative() {
        new WorkspacePath("/first").append("/second");
    }

    @Test(expected = IllegalArgumentException.class)
    public void append_null() {
        new WorkspacePath("/first").append((String) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void append_empty() {
        new WorkspacePath("/first").append("");
    }
    
    @Test
    public void append_ok() {
        assertThat(new WorkspacePath("/first").append("second").asPortableString(), equalTo("/first/second"));
    }
    
    @Test
    public void appendPath_absolute() {
        assertThat(new WorkspacePath("/first").append(new WorkspacePath("/second")), equalTo(new WorkspacePath("/first/second")));
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void appendPath_null() {
        new WorkspacePath("/first").append((WorkspacePath) null);
    }
    
    @Test
    public void appendPath_relative() {
        assertThat(new WorkspacePath("/first").append(new WorkspacePath("second/third")), equalTo(new WorkspacePath("/first/second/third")));
    }

    @Test
    public void appendPath_bothRelative() {
        assertThat(new WorkspacePath("first").append(new WorkspacePath("second/third")), equalTo(new WorkspacePath("first/second/third")));
    }
}

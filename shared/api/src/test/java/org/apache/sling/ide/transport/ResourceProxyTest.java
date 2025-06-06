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
package org.apache.sling.ide.transport;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public class ResourceProxyTest {
    
    private static final RepositoryPath PATH_CONTENT = new RepositoryPath("/content"); 

    @Test
    public void coveredChildren_firstLevel() {

        ResourceProxy r = new ResourceProxy(PATH_CONTENT);
        r.addChild(newResource("/content/test", "nt:unstructured"));

        assertThat(r.covers(new RepositoryPath("/content/test")), is(true));
    }

    @Test
    public void coveredChildren_secondLevel() {

        ResourceProxy r = new ResourceProxy(PATH_CONTENT);
        ResourceProxy child = newResource("/content/test", "nt:unstructured");
        r.addChild(child);

        child.addChild(newResource("/content/test/en", "nt:unstructured"));

        assertThat(r.covers(new RepositoryPath("/content/test/en")), is(true));
    }

    @Test
    public void coveredChildren_thirdLevel() {

        ResourceProxy r = new ResourceProxy(PATH_CONTENT);

        ResourceProxy child = newResource("/content/test", "nt:unstructured");
        r.addChild(child);

        ResourceProxy grandChild = newResource("/content/test/en", "nt:unstructured");
        child.addChild(grandChild);

        grandChild.addChild(newResource("/content/test/en/welcome", "nt:unstructured"));

        assertThat(r.covers(new RepositoryPath("/content/test/en/welcome")), is(true));
    }

    @Test
    public void coveredChildren_notCoveredFirstLevel() {

        ResourceProxy r = new ResourceProxy(PATH_CONTENT);
        r.addChild(new ResourceProxy(PATH_CONTENT.addChild("test")));

        assertThat(r.covers(new RepositoryPath("/content/test")), is(false));
    }

    @Test
    public void coveredChildren_notCoveredSecondLevel() {

        ResourceProxy r = new ResourceProxy(PATH_CONTENT);
        ResourceProxy child = newResource("/content/test", "nt:unstructured");
        r.addChild(child);

        child.addChild(new ResourceProxy(PATH_CONTENT.addChild("test").addChild("en")));

        assertThat(r.covers(new RepositoryPath("/content/test/en")), is(false));
    }

    @Test
    public void getChild() {

        ResourceProxy r = new ResourceProxy(PATH_CONTENT);
        ResourceProxy child = newResource("/content/test", "nt:unstructured");
        r.addChild(child);

        ResourceProxy grandChild = newResource("/content/test/en", "nt:unstructured");
        child.addChild(grandChild);

        ResourceProxy grandGrandChild = newResource("/content/test/en/welcome", "nt:unstructured");
        grandChild.addChild(grandGrandChild);

        assertThat(r.getChild(new RepositoryPath("/content/test")), is(child));
        assertThat(r.getChild(new RepositoryPath("/content/test/en")), is(grandChild));
        assertThat(r.getChild(new RepositoryPath("/content/test/en/welcome")), is(grandGrandChild));
        assertThat(r.getChild(new RepositoryPath("/content/test/en2")), is(nullValue()));
    }

    private ResourceProxy newResource(String path, String primaryType) {

        ResourceProxy child = new ResourceProxy(new RepositoryPath(path));
        child.addProperty("jcr:primaryType", primaryType);
        return child;
    }

    @Test(expected = IllegalArgumentException.class)
    public void addChild_IllegalChildRejected() {

        new ResourceProxy(new RepositoryPath("/content")).addChild(new ResourceProxy(new RepositoryPath("/var")));  
    }

    @Test
    public void addChild_childOfRootNode() {

        new ResourceProxy(new RepositoryPath("/")).addChild(new ResourceProxy(new RepositoryPath("/var")));
    }

    @Test
    public void addChild_childOfRegularNode() {

        new ResourceProxy(new RepositoryPath("/content")).addChild(new ResourceProxy(new RepositoryPath("/content/test")));
    }

    @Test(expected = IllegalArgumentException.class)
    public void addChild_deeplyNested() {

        new ResourceProxy(new RepositoryPath("/content")).addChild(new ResourceProxy(new RepositoryPath("/content/test/en")));
    }

}

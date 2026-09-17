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
package org.apache.sling.ide.test.impl;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;

import org.apache.sling.ide.eclipse.ui.nav.ProvisioningModelContentProvider;
import org.apache.sling.ide.test.impl.helpers.DisableDebugStatusHandlers;
import org.apache.sling.ide.test.impl.helpers.ProjectAdapter;
import org.apache.sling.ide.test.impl.helpers.TemporaryProject;
import org.eclipse.core.resources.IProject;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class ProvisioningModelContentProviderTest {

    @Rule
    public TemporaryProject projectRule = new TemporaryProject();

    @Rule
    public DisableDebugStatusHandlers disableDebugHandlers = new DisableDebugStatusHandlers();

    private IProject provisioningProject;
    
    @Before
    public void prepareProject() throws Exception {
        
        provisioningProject = projectRule.getProject();

        ProjectAdapter project = new ProjectAdapter(provisioningProject);
        project.addNatures("org.eclipse.wst.common.project.facet.core.nature");

        // install provisioning facet
        project.installFacet("sling.launchpad", "1.0");
    }

    @Test
    public void childrenOfProjectWithoutFeatureFolder() {
        
        ProvisioningModelContentProvider contentProvider = new ProvisioningModelContentProvider();
        assertArrayEquals(contentProvider.getChildren(provisioningProject), new Object[0]);
        assertFalse(contentProvider.hasChildren(provisioningProject));
    }

}

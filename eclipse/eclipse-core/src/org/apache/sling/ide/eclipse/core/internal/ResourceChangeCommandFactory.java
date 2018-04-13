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
package org.apache.sling.ide.eclipse.core.internal;

import java.io.IOException;

import org.apache.sling.ide.eclipse.core.EclipseResources;
import org.apache.sling.ide.transport.Command;
import org.apache.sling.ide.transport.Repository;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Status;

/**
 * The <tt>ResourceChangeCommandFactory</tt> creates new {@link #Command commands} correspoding to resource addition,
 * change, or removal
 *
 * @deprecated - Use the {@link DefaultCommandFactory} instead. This class is present until the tests are migrated off it
 */
@Deprecated
public class ResourceChangeCommandFactory {

    public Command<?> newCommandForAddedOrUpdated(Repository repository, IResource addedOrUpdated) throws CoreException {
        
        try {
            return Activator.getDefault().getCommandFactory().newCommandForAddedOrUpdatedResource(repository, EclipseResources.create(addedOrUpdated));
        } catch (IOException e) {
            throw new CoreException(new Status(Status.ERROR, Activator.PLUGIN_ID, "Failed updating " + addedOrUpdated,
                    e));
        }
    }

    public Command<?> newCommandForRemovedResources(Repository repository, IResource removed) throws CoreException {
        try {
            return  Activator.getDefault().getCommandFactory().newCommandForRemovedResource(repository, EclipseResources.create(removed));
        } catch (IOException e) {
            throw new CoreException(new Status(Status.ERROR, Activator.PLUGIN_ID, "Failed removing" + removed, e));
        }
    }

    public Command<Void> newReorderChildNodesCommand(Repository repository, IResource res) throws CoreException {
        try {
            return  Activator.getDefault().getCommandFactory().newReorderChildNodesCommand(repository, EclipseResources.create(res));
        } catch (IOException e) {
            throw new CoreException(new Status(Status.ERROR, Activator.PLUGIN_ID, "Failed reordering child nodes for "
                    + res, e));
        }
    }

}

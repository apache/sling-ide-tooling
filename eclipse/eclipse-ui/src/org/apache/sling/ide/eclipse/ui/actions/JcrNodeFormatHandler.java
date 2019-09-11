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
package org.apache.sling.ide.eclipse.ui.actions;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.jackrabbit.vault.fs.io.DocViewFormat;
import org.apache.jackrabbit.vault.util.xml.serialize.XMLSerializer;
import org.apache.sling.ide.eclipse.ui.internal.Activator;
import org.apache.sling.ide.eclipse.ui.internal.SelectionUtils;
import org.apache.sling.ide.eclipse.ui.nav.model.JcrNode;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.handlers.HandlerUtil;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

public class JcrNodeFormatHandler extends AbstractHandler {
    
    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        run(HandlerUtil.getCurrentSelection(event));
        return null;
    }

    private void run(ISelection currentSelection) {
        JcrNode node = SelectionUtils.getFirst(currentSelection, JcrNode.class);
        if (node == null)
            return;

        IFile resource = node.getFileForEditor();
        if ( resource == null )
            return;
        
        IPath resourcePath = resource.getFullPath();
        Job job = new Job(NLS.bind("Formatting {0}", resourcePath)) {
            
            @Override
            protected IStatus run(IProgressMonitor monitor) {
                try {
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    XMLSerializer serializer = new XMLSerializer(out, new DocViewFormat().getXmlOutputFormat());
                    XMLReader reader = XMLReaderFactory.createXMLReader();
                    reader.setContentHandler(serializer);
                    reader.setDTDHandler(serializer);
                    reader.parse(new InputSource(resource.getContents()));
                    
                    resource.setContents(new ByteArrayInputStream(out.toByteArray()), false, true, monitor);
                } catch (SAXException | IOException | CoreException e) {
                    return new Status(IStatus.ERROR, Activator.PLUGIN_ID, NLS.bind("Failed formatting {0}", resourcePath), e);
                }
                
                return Status.OK_STATUS;

            }
        };
        job.setPriority(Job.SHORT);
        job.setRule(resource);
        job.schedule();
        
    }
}
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
package org.apache.sling.ide.eclipse.ui.internal;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.internal.ui.DebugPluginImages;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.DecorationOverlayIcon;
import org.eclipse.jface.viewers.IDecoration;

/**
 * The <tt>SharedImages</tt> class contains references to images
 * 
 */
@SuppressWarnings("restriction")
public final class SharedImages {
    
    public static final ImageDescriptor SLING_LOG = ImageDescriptor.createFromFile(SharedImages.class, "sling-logo.png");
    public static final ImageDescriptor SLING_ICON = ImageDescriptor.createFromFile(SharedImages.class, "sling.gif");
    public static final ImageDescriptor NT_UNSTRUCTURED_ICON = ImageDescriptor.createFromFile(SharedImages.class, "unstructured.png");
    public static final ImageDescriptor CONTENT_OVERLAY = ImageDescriptor.createFromURL(FileLocator.find(Activator
            .getDefault().getBundle(), Path.fromPortableString("icons/ovr16/content_ovr.gif"), null));
    
    public static final ImageDescriptor DISCONNECT = DebugPluginImages.getImageDescriptor(IDebugUIConstants.IMG_LCL_DISCONNECT);
    public static final ImageDescriptor RUN_CONNECT = new DecorationOverlayIcon(DebugPluginImages.getImageDescriptor(IDebugUIConstants.IMG_ACT_RUN).createImage(), ImageDescriptor.createFromFile(SharedImages.class, "connectOverlay.png"), IDecoration.BOTTOM_RIGHT);
    public static final ImageDescriptor DEBUG_CONNECT = new DecorationOverlayIcon(DebugPluginImages.getImageDescriptor(IDebugUIConstants.IMG_ACT_DEBUG).createImage(), ImageDescriptor.createFromFile(SharedImages.class, "connectOverlay.png"), IDecoration.BOTTOM_RIGHT);

    private SharedImages() {
    }

}

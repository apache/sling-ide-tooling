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

import org.apache.sling.ide.eclipse.ui.nav.model.JcrNode;
import org.eclipse.core.expressions.PropertyTester;

public class ContentXmlResourceTester extends PropertyTester {

    // this class should be in the core project, but since the JcrNode class is in the ui project
    // we keep it here
    private static final String PN_IS_CONTENT_XML = "isContentXmlFile";

    @Override
    public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
        
        if (!PN_IS_CONTENT_XML.equals(property)) {
            return false;
        }

        if (!(receiver instanceof JcrNode)) {
            return false;
        }
        
        JcrNode node = (JcrNode) receiver;
        
        return node.getFileForEditor() != null;
    }

}

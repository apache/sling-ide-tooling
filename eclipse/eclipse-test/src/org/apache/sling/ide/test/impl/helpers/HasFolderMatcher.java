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
package org.apache.sling.ide.test.impl.helpers;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

public class HasFolderMatcher extends TypeSafeMatcher<IProject> {

    public static final HasFolderMatcher hasFolder(String folderPath) {
        return new HasFolderMatcher(folderPath);
    }

    private final String folderName;


    public HasFolderMatcher(String folderPath) {
        this.folderName = folderPath;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("project with a folder located at " + folderName);
    }

    @Override
    protected void describeMismatchSafely(IProject item, Description mismatchDescription) {
        mismatchDescription.appendText("at location ").appendText(folderName).appendText(" found member ")
                .appendValue(item.findMember(folderName));
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.hamcrest.TypeSafeMatcher#matchesSafely(java.lang.Object)
     */
    @Override
    protected boolean matchesSafely(IProject item) {
        if (item == null) {
            return false;
        }

        IResource maybeFolder = item.findMember(folderName);

        return maybeFolder != null && maybeFolder.getType() == IResource.FOLDER;
    }

}
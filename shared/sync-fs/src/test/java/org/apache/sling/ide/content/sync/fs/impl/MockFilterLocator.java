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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;

import org.apache.sling.ide.filter.Filter;
import org.apache.sling.ide.filter.FilterLocator;
import org.apache.sling.ide.filter.FilterResult;
import org.apache.sling.ide.transport.RepositoryPath;

public class MockFilterLocator implements FilterLocator {

    public static final Filter MOCK_FILTER = new Filter() {
        
        @Override
        public FilterResult filter(RepositoryPath repositoryPath) {
            return FilterResult.ALLOW;
        }
    };
    
    @Override
    public File findFilterLocation(File syncDirectory) {
        
        return syncDirectory.getParentFile().toPath().
                resolve(Paths.get("META-INF", "vault", "filter.xml")).toFile();
        
    }

    @Override
    public Filter loadFilter(InputStream filterFileContents) throws IOException {
        if ( filterFileContents == null )
            return null;
        return MOCK_FILTER;
    }

}

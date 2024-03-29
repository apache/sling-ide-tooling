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
package org.apache.sling.ide.impl.vlt;

import java.util.HashMap;
import java.util.Map;

import org.apache.sling.ide.log.Logger;
import org.apache.sling.ide.transport.Repository;
import org.apache.sling.ide.transport.RepositoryException;
import org.apache.sling.ide.transport.RepositoryFactory;
import org.apache.sling.ide.transport.RepositoryInfo;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.component.propertytypes.ServiceRanking;

/**
 * The <tt>VltRepositoryFactory</tt> instantiates <tt>VltRepository</tt> instances
 * This service should get precedence over {@code org.apache.sling.ide.impl.resource.transport.RepositoryFactoryImpl} in
 * case both are deployed in an OSGi container.
 */
@Component(service = RepositoryFactory.class)
@ServiceRanking(1000)
public class VltRepositoryFactory implements RepositoryFactory {

    private final Logger logger;
    
    private Map<String,VltRepository> repositoryMap = new HashMap<>();

    /**
     * Constructor to create this instance
     *
     * @param logger Sling IDE Logger which must not be null
     */
    @Activate
    public VltRepositoryFactory(@Reference Logger logger) {
        this.logger = logger;
    }

    @Override
    public Repository getRepository(RepositoryInfo repositoryInfo,
            boolean acceptsDisconnectedRepository) throws RepositoryException {

        final String key = getKey(repositoryInfo);
        
        synchronized(repositoryMap) {
            VltRepository repo = repositoryMap.get(key);
            if (repo==null) {
                return null;
            }
            if (!repo.isDisconnected() || acceptsDisconnectedRepository) {
                return repo;
            }
        }
        return null;
    }
    
    
    @Override
    public Repository connectRepository(RepositoryInfo repositoryInfo) throws RepositoryException {

        final String key = getKey(repositoryInfo);
        
        synchronized(repositoryMap) {
            VltRepository repo = repositoryMap.get(key);
            if (repo!=null && !repo.isDisconnected()) {
                return repo;
            }
            
            repo = new VltRepository(repositoryInfo, logger);
            repo.connect();
            
            repositoryMap.put(key, repo);
            return repo;
        }
    }
    
    @Override
    public void disconnectRepository(RepositoryInfo repositoryInfo) {
        final String key = getKey(repositoryInfo);
        synchronized(repositoryMap) {
            VltRepository r = repositoryMap.get(key);
            // marking the repository as disconnected allows us to keep using it
            // (eg for node type registry lookups) although the server is stopped
            //TODO we might come up with a proper online/offline handling here
            if ( r != null ) {
            	r.disconnected();
            }
        }
    }

    private String getKey(RepositoryInfo repositoryInfo) {
        return repositoryInfo.getUsername()+":"+repositoryInfo.getPassword()+"@"+repositoryInfo.getUrl();
    }
}

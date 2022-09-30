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
package org.apache.sling.ide.impl.resource.transport;

import org.apache.sling.ide.log.Logger;
import org.apache.sling.ide.transport.Repository;
import org.apache.sling.ide.transport.RepositoryException;
import org.apache.sling.ide.transport.RepositoryFactory;
import org.apache.sling.ide.transport.RepositoryInfo;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * The <tt>RepositoryFactoryImpl</tt> creates <tt>RepositoryImpl</tt> instances
 *
 */
@Component(service = RepositoryFactory.class)
public class RepositoryFactoryImpl implements RepositoryFactory {

	private final Logger logger;

    /**
     * Constructor to create this instance
     *
     * @param logger the logger
     */
	@Activate
    public RepositoryFactoryImpl(@Reference Logger logger) {
        this.logger = logger;
    }

    @Override
    public Repository connectRepository(RepositoryInfo repositoryInfo) throws RepositoryException {
        //TODO: currently not doing repository-caching
        return new RepositoryImpl(repositoryInfo, logger);
    }
    
    @Override
    public Repository getRepository(RepositoryInfo repositoryInfo,
            boolean acceptsDisconnectedRepository) throws RepositoryException {
        //TODO: currently not doing repository-caching
        return new RepositoryImpl(repositoryInfo, logger);
    }
    
    @Override
    public void disconnectRepository(RepositoryInfo repositoryInfo) {
        //TODO: not yet implemented
    }

}

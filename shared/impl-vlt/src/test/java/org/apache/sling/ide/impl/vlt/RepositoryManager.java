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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Properties;

import javax.jcr.Credentials;
import javax.jcr.LoginException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.nodetype.InvalidNodeTypeDefinitionException;
import javax.jcr.nodetype.NodeTypeExistsException;

import org.apache.jackrabbit.api.JackrabbitRepository;
import org.apache.jackrabbit.commons.cnd.CndImporter;
import org.apache.jackrabbit.commons.cnd.ParseException;
import org.apache.jackrabbit.commons.jackrabbit.authorization.AccessControlUtils;
import org.apache.jackrabbit.oak.jcr.Jcr;
import org.apache.jackrabbit.oak.plugins.tree.impl.RootProviderService;
import org.apache.jackrabbit.oak.plugins.tree.impl.TreeProviderService;
import org.apache.jackrabbit.oak.security.internal.SecurityProviderBuilder;
import org.apache.jackrabbit.oak.security.user.RandomAuthorizableNodeName;
import org.apache.jackrabbit.oak.spi.security.ConfigurationParameters;
import org.apache.jackrabbit.oak.spi.security.SecurityProvider;
import org.apache.jackrabbit.oak.spi.security.authorization.AuthorizationConfiguration;
import org.apache.jackrabbit.oak.spi.security.principal.EveryonePrincipal;
import org.apache.jackrabbit.oak.spi.security.privilege.PrivilegeConstants;
import org.apache.jackrabbit.oak.spi.security.user.AuthorizableNodeName;
import org.apache.jackrabbit.oak.spi.security.user.UserConfiguration;
import org.apache.jackrabbit.oak.spi.security.user.UserConstants;
import org.apache.jackrabbit.oak.spi.security.user.action.AccessControlAction;
import org.apache.jackrabbit.oak.spi.xml.ImportBehavior;
import org.apache.jackrabbit.oak.spi.xml.ProtectedItemImporter;
import org.junit.Rule;
import org.junit.rules.ExternalResource;

/**
 * Creates an in memory Oak repository.
 * To be used as JUnit {@link Rule}.
 */
public class RepositoryManager extends ExternalResource {

	private Repository repository;
	private Session adminSession;

	public RepositoryManager() {

	}

	public Repository getRepository() throws RepositoryException, ParseException, IOException {
		if (repository == null) {
			repository = createOakRepository();
		}
		return repository;
	}

	/**
	 * @return the singleton admin session which is automatically closed at the end
	 *         of the test
	 * @throws LoginException
	 * @throws RepositoryException
	 * @throws IOException 
	 * @throws ParseException 
	 */
	public Session getAdminSession() throws LoginException, RepositoryException, ParseException, IOException {
		if (adminSession == null) {
			adminSession = getRepository().login(getAdminCredentials());
		}
		return adminSession;
	}

	/**
	 * @return a new admin session
	 * @throws LoginException
	 * @throws RepositoryException
	 * @throws IOException 
	 * @throws ParseException 
	 */
	public Session createAdminSession() throws LoginException, RepositoryException, ParseException, IOException {
		return getRepository().login(getAdminCredentials());
	}

	public Credentials getAdminCredentials() {
		return new SimpleCredentials("admin", "admin".toCharArray());
	}

	Repository createOakRepository() throws RepositoryException, ParseException, IOException {
		// in-memory repo
		Jcr jcr = new Jcr();

		// TODO: optimize by disabling JMX with
		// https://issues.apache.org/jira/browse/OAK-9959
		Repository repository = jcr.with(createSecurityProvider()).withAtomicCounter().createRepository();

		// setup default read ACL for everyone
		Session admin = repository.login(new SimpleCredentials("admin", "admin".toCharArray()));
		AccessControlUtils.addAccessControlEntry(admin, "/", EveryonePrincipal.getInstance(),
				new String[] { "jcr:read" }, true);
		
		// add some more node type definitions
		importNodeTypeDefinitions(admin, "test-definitions.cnd");
        importNodeTypeDefinitions(admin, "folder.cnd");
        
		admin.save();
		admin.logout();

		return repository;
	}

	private void importNodeTypeDefinitions(Session session, String cndResourceName)
			throws InvalidNodeTypeDefinitionException, NodeTypeExistsException, UnsupportedRepositoryOperationException,
			ParseException, RepositoryException, IOException {
		try (InputStream cndInput = getClass().getResourceAsStream(cndResourceName)) {
			if (cndInput == null) {
				throw new IllegalArgumentException("Unable to read classpath resource " + cndResourceName);
			}
			CndImporter.registerNodeTypes(new InputStreamReader(cndInput), session);
		}
	}

	static SecurityProvider createSecurityProvider() {
		SecurityProvider securityProvider = SecurityProviderBuilder.newBuilder()
				.with(getSecurityConfigurationParameters()).withRootProvider(new RootProviderService())
				.withTreeProvider(new TreeProviderService()).build();
		return securityProvider;
	}

	static ConfigurationParameters getSecurityConfigurationParameters() {
		Properties userProps = new Properties();
		AuthorizableNodeName nameGenerator = new RandomAuthorizableNodeName();

		userProps.put(UserConstants.PARAM_USER_PATH, "/home/users");
		userProps.put(UserConstants.PARAM_GROUP_PATH, "/home/groups");
		userProps.put(AccessControlAction.USER_PRIVILEGE_NAMES, new String[] { PrivilegeConstants.JCR_ALL });
		userProps.put(AccessControlAction.GROUP_PRIVILEGE_NAMES, new String[] { PrivilegeConstants.JCR_READ });
		userProps.put(ProtectedItemImporter.PARAM_IMPORT_BEHAVIOR, ImportBehavior.NAME_BESTEFFORT);
		userProps.put(UserConstants.PARAM_AUTHORIZABLE_NODE_NAME, nameGenerator);
		userProps.put("cacheExpiration", 3600 * 1000);
		Properties authzProps = new Properties();
		authzProps.put(ProtectedItemImporter.PARAM_IMPORT_BEHAVIOR, ImportBehavior.NAME_BESTEFFORT);
		return ConfigurationParameters.of(UserConfiguration.NAME, ConfigurationParameters.of(userProps),
				AuthorizationConfiguration.NAME, ConfigurationParameters.of(authzProps));
	}

	@Override
	protected void after() {
		if (repository != null) {
			if (adminSession != null && adminSession.isLive()) {
				adminSession.logout();
			}
			if (repository instanceof JackrabbitRepository) {
				((JackrabbitRepository) repository).shutdown();
			}
		}
	}

}

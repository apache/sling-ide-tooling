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

import static org.apache.sling.ide.transport.Repository.CommandExecutionFlag.CREATE_ONLY_WHEN_MISSING;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.Session;
import javax.jcr.Value;

import org.apache.jackrabbit.vault.util.JcrConstants;
import org.apache.sling.ide.filter.Filter;
import org.apache.sling.ide.filter.FilterResult;
import org.apache.sling.ide.log.Logger;
import org.apache.sling.ide.transport.CommandContext;
import org.apache.sling.ide.transport.ResourceProxy;
import org.hamcrest.Matchers;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

public class AddOrUpdateNodeCommandIT {

	private static final CommandContext DEFAULT_CONTEXT = new CommandContext(new Filter() {
		@Override
		public FilterResult filter(String repositoryPath) {
			return FilterResult.ALLOW;
		}
	});

	@Rule
	public final RepositoryManager repositoryManager = new RepositoryManager();

	private static final String PROP_NAME = "jcr:title";

	private Logger logger = new Slf4jLogger();

	@Test
	public void setProperty() throws Exception {

		doPropertyChangeTest(null, "Title");
	}

	private ResourceProxy newResource(String path, String primaryType) {

		ResourceProxy resource = new ResourceProxy(path);
		resource.addProperty("jcr:primaryType", primaryType);
		return resource;
	}

	
	private void doPropertyChangeTest(final Object initialPropertyValues, final Object newPropertyValues)
			throws Exception {

		Session session = repositoryManager.getAdminSession();
		Node contentNode = session.getRootNode().addNode("content");
		if (initialPropertyValues instanceof String) {
			contentNode.setProperty(PROP_NAME, (String) initialPropertyValues);
		} else if (initialPropertyValues instanceof String[]) {
			contentNode.setProperty(PROP_NAME, (String[]) initialPropertyValues);
		}

		session.save();

		ResourceProxy resource = newResource("/content", JcrConstants.NT_UNSTRUCTURED);
		if (newPropertyValues != null) {
			resource.addProperty(PROP_NAME, newPropertyValues);
		}

		AddOrUpdateNodeCommand cmd = new AddOrUpdateNodeCommand(repositoryManager.getRepository(),
				repositoryManager.getAdminCredentials(), DEFAULT_CONTEXT, null, resource, logger);
		cmd.execute().get();

		session.refresh(false);

		if (newPropertyValues == null) {
			assertThat(session.getNode("/content").hasProperty(PROP_NAME), equalTo(false));
			return;
		}

		Property newProp = session.getNode("/content").getProperty(PROP_NAME);
		if (newPropertyValues instanceof String) {
			assertThat("property.isMultiple", newProp.isMultiple(), equalTo(Boolean.FALSE));
			assertThat(newProp.getString(), equalTo((String) newPropertyValues));

		} else {

			String[] expectedValues = (String[]) newPropertyValues;
			assertThat("property.isMultiple", newProp.isMultiple(), equalTo(Boolean.TRUE));

			Value[] values = session.getNode("/content").getProperty(PROP_NAME).getValues();

			assertThat(values.length, equalTo(expectedValues.length));
			for (int i = 0; i < values.length; i++) {
				assertThat(values[i].getString(), equalTo(expectedValues[i]));
			}

		}

	}

	@Test
	public void removeProperty() throws Exception {

		doPropertyChangeTest("Title", null);
	}

	@Test
	public void singlePropertyToMultiValued() throws Exception {

		doPropertyChangeTest("Title", new String[] { "Title", "Title 2" });
	}

	@Test
	public void multiValuesPropertyToSingle() throws Exception {

		doPropertyChangeTest(new String[] { "Title", "Title 2" }, "Title");
	}

	
	@Test
	public void changeNtFolderToSlingFolderWithAddedProperty() throws Exception {

		Session session = repositoryManager.getAdminSession();
		session.getRootNode().addNode("content", "nt:folder");

		session.save();

		ResourceProxy resource = newResource("/content", "sling:Folder");
		resource.getProperties().put("newProperty", "some/value");

		AddOrUpdateNodeCommand cmd = new AddOrUpdateNodeCommand(repositoryManager.getRepository(), repositoryManager.getAdminCredentials(), DEFAULT_CONTEXT, null,
				resource, logger);
		cmd.execute().get();

		session.refresh(false);

		Node content = session.getRootNode().getNode("content");
		assertThat(content.getPrimaryNodeType().getName(), equalTo("sling:Folder"));

	}

	@Test
	public void changeSlingFolderToNtFolderWithExistingProperty() throws Exception {
		Session session = repositoryManager.getAdminSession();
		Node content = session.getRootNode().addNode("content", "sling:Folder");
		content.setProperty("newProperty", "some/value");

		session.save();

		ResourceProxy resource = newResource("/content", "nt:folder");

		AddOrUpdateNodeCommand cmd = new AddOrUpdateNodeCommand(repositoryManager.getRepository(), repositoryManager.getAdminCredentials(), DEFAULT_CONTEXT, null,
				resource, logger);
		cmd.execute().get();

		session.refresh(false);

		content = session.getRootNode().getNode("content");
		assertThat(content.getPrimaryNodeType().getName(), equalTo("nt:folder"));

	}

	@Test
	@Ignore("SLING-4036")
	public void updateNtUnstructuredToNodeWithRequiredProperty() throws Exception {

		Session session = repositoryManager.getAdminSession();
		Node content = session.getRootNode().addNode("content", "nt:unstructured");

		session.save();

		ResourceProxy resource = newResource("/content", "custom");
		resource.getProperties().put("attribute", "some value");

		AddOrUpdateNodeCommand cmd = new AddOrUpdateNodeCommand(repositoryManager.getRepository(), repositoryManager.getAdminCredentials(), DEFAULT_CONTEXT, null,
				resource, logger);
		cmd.execute().get();

		session.refresh(false);

		content = session.getRootNode().getNode("content");
		assertThat(content.getPrimaryNodeType().getName(), equalTo("custom"));

	}

	@Test
	public void nodeNotPresentButOutsideOfFilterIsNotRemoved() throws Exception {

		final CommandContext context = new CommandContext(new Filter() {

			@Override
			public FilterResult filter(String repositoryPath) {
				if (repositoryPath.equals("/content/not-included-child")) {
					return FilterResult.DENY;
				}

				return FilterResult.ALLOW;
			}
		});

		Session session = repositoryManager.getAdminSession();
		Node content = session.getRootNode().addNode("content", "nt:unstructured");
		content.addNode("included-child");
		content.addNode("not-included-child");

		session.save();

		ResourceProxy resource = newResource("/content", "nt:unstructured");
		resource.addChild(newResource("/content/included-child", "nt:unstructured"));

		AddOrUpdateNodeCommand cmd = new AddOrUpdateNodeCommand(repositoryManager.getRepository(), repositoryManager.getAdminCredentials(), context, null, resource,
				logger);
		cmd.execute().get();

		session.refresh(false);

		content = session.getRootNode().getNode("content");
		content.getNode("included-child");
		content.getNode("not-included-child");

	}

	
	@Test
	public void createIfRequiredFlagSkipsExistingResources() throws Exception {

		Session session = repositoryManager.getAdminSession();
		Node content = session.getRootNode().addNode("content", "nt:folder");

		session.save();

		ResourceProxy resource = newResource("/content", "nt:unstructured");

		AddOrUpdateNodeCommand cmd = new AddOrUpdateNodeCommand(repositoryManager.getRepository(), repositoryManager.getAdminCredentials(), DEFAULT_CONTEXT, null,
				resource, logger, CREATE_ONLY_WHEN_MISSING);
		cmd.execute().get();

		session.refresh(false);

		content = session.getRootNode().getNode("content");
		assertThat(content.getPrimaryNodeType().getName(), equalTo("nt:folder"));

	}

	@Test
	public void createIfRequiredFlagCreatesNeededResources() throws Exception {

		Session session = repositoryManager.getAdminSession();
		ResourceProxy resource = newResource("/content", "nt:unstructured");

		AddOrUpdateNodeCommand cmd = new AddOrUpdateNodeCommand(repositoryManager.getRepository(), repositoryManager.getAdminCredentials(), DEFAULT_CONTEXT, null,
				resource, logger, CREATE_ONLY_WHEN_MISSING);
		cmd.execute().get();

		session.refresh(false);

		Node content = session.getRootNode().getNode("content");
		assertThat(content.getPrimaryNodeType().getName(), equalTo("nt:unstructured"));

	}

	@Test
	public void createIfRequiredFlagCreatesNeededResourcesEvenWhenPrimaryTypeIsMissing() throws Exception {

		Session session = repositoryManager.getAdminSession();
		ResourceProxy resource = new ResourceProxy("/content");

		AddOrUpdateNodeCommand cmd = new AddOrUpdateNodeCommand(repositoryManager.getRepository(), repositoryManager.getAdminCredentials(), DEFAULT_CONTEXT, null,
				resource, logger, CREATE_ONLY_WHEN_MISSING);
		cmd.execute().get();

		session.refresh(false);

		Node content = session.getRootNode().getNode("content");
		assertThat(content.getPrimaryNodeType().getName(), equalTo("nt:unstructured"));

	}

	@Test
	public void autoCreatedPropertiesAreNotRemoved() throws Exception {

		Session session = repositoryManager.getAdminSession();
		Node content = session.getRootNode().addNode("content", "nt:folder");

		session.save();

		ResourceProxy resource = newResource("/content", "nt:folder");
		resource.addProperty("jcr:mixinTypes", "mix:lastModified");

		AddOrUpdateNodeCommand cmd = new AddOrUpdateNodeCommand(repositoryManager.getRepository(), repositoryManager.getAdminCredentials(), DEFAULT_CONTEXT, null,
				resource, logger);
		cmd.execute().get();
		cmd.execute().get(); // second time since mixins are processed after properties so we need two
								// executions to
								// expose the problem

		session.refresh(false);

		content = session.getRootNode().getNode("content");
		assertThat("jcr:lastModified property not present", content.hasProperty("jcr:lastModified"),
				equalTo(true));
		assertThat("jcr:lastModifiedBy property not present", content.hasProperty("jcr:lastModifiedBy"),
				equalTo(true));

	}

	@Test
	public void autoCreatedPropertiesAreUpdatedIfPresent() throws Exception {

		Session session = repositoryManager.getAdminSession();
		Node content = session.getRootNode().addNode("content", "nt:folder");

		session.save();

		ResourceProxy resource = newResource("/content", "nt:folder");
		resource.addProperty("jcr:mixinTypes", "mix:lastModified");
		resource.addProperty("jcr:lastModifiedBy", "admin2");

		AddOrUpdateNodeCommand cmd = new AddOrUpdateNodeCommand(repositoryManager.getRepository(), repositoryManager.getAdminCredentials(), DEFAULT_CONTEXT, null,
				resource, logger);
		cmd.execute().get();
		cmd.execute().get(); // second time since mixins are processed after properties so we need two
								// executions to
								// expose the problem

		session.refresh(false);

		content = session.getRootNode().getNode("content");
		assertThat("jcr:lastModifiedBy property not modified",
				content.getProperty("jcr:lastModifiedBy").getString(), equalTo("admin2"));

	}

	@Test
	public void setEmptyMixinTypes() throws Exception {
		setMixinTypes0();
	}

	private void setMixinTypes0(final String... mixinTypeNames) throws Exception {

		Session session = repositoryManager.getAdminSession();
		ResourceProxy resource = newResource("/content", "nt:unstructured");
		resource.addProperty("jcr:mixinTypes", mixinTypeNames);

		AddOrUpdateNodeCommand cmd = new AddOrUpdateNodeCommand(repositoryManager.getRepository(),
				repositoryManager.getAdminCredentials(), DEFAULT_CONTEXT, null,
				resource, logger);
		cmd.execute().get();

		session.refresh(false);

		Node content = session.getRootNode().getNode("content");
		assertThat(content.getMixinNodeTypes(), Matchers.arrayWithSize(mixinTypeNames.length));

	}

	@Test
	public void setMixinTypes() throws Exception {

		setMixinTypes0("mix:created");
	}

}

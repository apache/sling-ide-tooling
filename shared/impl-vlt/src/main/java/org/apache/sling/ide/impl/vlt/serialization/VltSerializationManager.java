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
package org.apache.sling.ide.impl.vlt.serialization;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.apache.jackrabbit.util.Text;
import org.apache.jackrabbit.vault.fs.Mounter;
import org.apache.jackrabbit.vault.fs.api.RepositoryAddress;
import org.apache.jackrabbit.vault.fs.api.VaultFile;
import org.apache.jackrabbit.vault.fs.api.VaultFileSystem;
import org.apache.jackrabbit.vault.fs.io.DocViewParser;
import org.apache.jackrabbit.vault.fs.io.DocViewParser.XmlParseException;
import org.apache.jackrabbit.vault.util.Constants;
import org.apache.jackrabbit.vault.util.PlatformNameFormat;
import org.apache.jackrabbit.vault.util.RepositoryProvider;
import org.apache.sling.ide.impl.vlt.VaultFsLocator;
import org.apache.sling.ide.log.Logger;
import org.apache.sling.ide.serialization.SerializationDataBuilder;
import org.apache.sling.ide.serialization.SerializationException;
import org.apache.sling.ide.serialization.SerializationKind;
import org.apache.sling.ide.serialization.SerializationManager;
import org.apache.sling.ide.sync.content.WorkspaceDirectory;
import org.apache.sling.ide.sync.content.WorkspaceFile;
import org.apache.sling.ide.sync.content.WorkspacePath;
import org.apache.sling.ide.sync.content.WorkspaceProject;
import org.apache.sling.ide.sync.content.WorkspaceResource;
import org.apache.sling.ide.transport.RepositoryPath;
import org.apache.sling.ide.transport.ResourceProxy;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.xml.sax.InputSource;

@Component(service = SerializationManager.class)
public class VltSerializationManager implements SerializationManager {

    static final String EXTENSION_XML = ".xml";

    private final Logger logger;
    private final VaultFsLocator fsLocator;

    public static void main(String[] args) throws RepositoryException, URISyntaxException, IOException {
        RepositoryAddress address = new RepositoryAddress("http://localhost:8080/server/root");
        Repository repo = new RepositoryProvider().getRepository(address);
        Session session = repo.login(new SimpleCredentials("admin", "admin".toCharArray()));

        VaultFileSystem fs = Mounter.mount(null, null, address, "/", session);

        String[] attempts = new String[] { "/rep:policy", "/var" };

        for (String attempt : attempts) {
            VaultFile vaultFile = fs.getFile(attempt);

            System.out.println(attempt + " -> " + vaultFile);
        }

        for (String attempt : attempts) {

            attempt = PlatformNameFormat.getPlatformPath(attempt) + EXTENSION_XML;

            VaultFile vaultFile = fs.getFile(attempt);

            System.out.println(attempt + " -> " + vaultFile);
        }

    }

  
    /**
     * Constructor to create this instance
     *
     * @param logger Sling IDE Logger which must not be null
     * @param fsLocator Vault File System Locator which must not be null
     */
    @Activate
    public VltSerializationManager(@Reference Logger logger, @Reference VaultFsLocator fsLocator) {
        this.logger = logger;
        this.fsLocator = fsLocator;
    }

    @Override
    public void destroy() {

    }

    @Override
    public boolean isSerializationFile(WorkspaceFile file) {
        
        String fileName = file.getName();
        if (fileName.equals(Constants.DOT_CONTENT_XML)) {
            return true;
        }

        if (!fileName.endsWith(EXTENSION_XML)) {
            return false;
        }

        // TODO - refrain from doing I/O here
        // TODO - copied from TransactionImpl
        
        try (InputStream in = file.getContents()) {
            return DocViewParser.isDocView(new InputSource(in));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public WorkspaceFile getSerializationFile(WorkspaceResource resource, SerializationKind serializationKind) {

        switch (serializationKind) {
            case FOLDER:
            case METADATA_PARTIAL:
                // normally we would assume that the resource is a WorkspaceDirectory. However, the API contract is quite
                // loose and we normally receive WorkspaceFile instances as well. So we rebuild an equivalent WorkspaceDirectory
                // object to allow the logic to work in all cases.
                WorkspacePath path = resource.getLocalPath();
                WorkspacePath projectRelativePath = resource.getProject().getLocalPath().relativize(path);
                return resource.getProject().getDirectory(projectRelativePath)
                        .getFile(new WorkspacePath(Constants.DOT_CONTENT_XML));
            case METADATA_FULL:
                return (WorkspaceFile) resource;
            case FILE:
                WorkspaceProject project = resource.getProject();
                WorkspaceDirectory parentDir = project.getDirectory(resource.getLocalPath().getParent());
                return parentDir.getDirectory(new WorkspacePath(resource.getName() + ".dir"))
                        .getFile(new WorkspacePath(Constants.DOT_CONTENT_XML));
        }

        throw new IllegalArgumentException("Unsupported serialization kind " + serializationKind);
    }

    @Override
    public RepositoryPath getRepositoryPath(WorkspacePath localPath) {

        String osPath = localPath.asPortableString();
        String repositoryPath;
        String name = Text.getName(osPath);
        if (name.equals(Constants.DOT_CONTENT_XML)) {
            // TODO - this is a bit risky, we might clean legitimate directories which contain '.dir'
            String parentPath = Text.getRelativeParent(osPath, 1);
            if (parentPath != null && parentPath.endsWith(".dir")) {
                parentPath = parentPath.substring(0, parentPath.length() - ".dir".length());
            }
            repositoryPath = PlatformNameFormat.getRepositoryPath(parentPath);
        } else {
            // TODO - we assume here that it's a full coverage aggregate but it might not be
            if (osPath.endsWith(EXTENSION_XML)) {
                repositoryPath = PlatformNameFormat.getRepositoryPath(osPath.substring(0, osPath.length()
                        - EXTENSION_XML.length()));
            } else {
                repositoryPath = PlatformNameFormat.getRepositoryPath(osPath).replace(".dir/", "/");
            }
        }

        // TODO extract into PathUtils
        if (repositoryPath.length() > 0 && repositoryPath.charAt(0) != '/') {
            repositoryPath = '/' + repositoryPath;
        } else if (repositoryPath.length() == 0) {
            repositoryPath = "/";
        }

        return new RepositoryPath(repositoryPath);
    }

    @Override
    public String getLocalName(String repositoryName) {
        if (repositoryName == null || repositoryName.contains("/"))
            throw new IllegalArgumentException("Repository name must not be null or contain slashes : " + repositoryName);
        
        return PlatformNameFormat.getPlatformName(repositoryName);
    }
    
    @Override
    public SerializationDataBuilder newBuilder(
    		org.apache.sling.ide.transport.Repository repository,
    		File contentSyncRoot) throws SerializationException {
        VltSerializationDataBuilder b = new VltSerializationDataBuilder(logger, fsLocator);
    	b.init(repository, contentSyncRoot);
    	return b;
    }

    @Override
    public ResourceProxy readSerializationData(WorkspaceFile file) throws IOException {
        if (file == null || ! file.exists() )
            return null;

        RepositoryPath repositoryPath = getRepositoryPath(file.getPathRelativeToSyncDir());

        try (InputStream source = file.getContents()) {
        	DocViewParser parser = new DocViewParser();
        	ResourceProxyParserHandler handler = new ResourceProxyParserHandler();
        	parser.parse(repositoryPath.asString(), new InputSource(source), handler);
            return handler.getRoot();
        } catch (XmlParseException e) {
            // TODO proper error handling
            throw new IOException(e);
        }
    }
}

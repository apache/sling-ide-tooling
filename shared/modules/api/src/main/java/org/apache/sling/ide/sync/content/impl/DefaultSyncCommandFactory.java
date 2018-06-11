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
package org.apache.sling.ide.sync.content.impl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.sling.ide.filter.FilterResult;
import org.apache.sling.ide.log.Logger;
import org.apache.sling.ide.serialization.SerializationKind;
import org.apache.sling.ide.serialization.SerializationKindManager;
import org.apache.sling.ide.serialization.SerializationManager;
import org.apache.sling.ide.sync.content.SyncCommandFactory;
import org.apache.sling.ide.sync.content.WorkspaceDirectory;
import org.apache.sling.ide.sync.content.WorkspaceFile;
import org.apache.sling.ide.sync.content.WorkspacePath;
import org.apache.sling.ide.sync.content.WorkspacePaths;
import org.apache.sling.ide.sync.content.WorkspaceResource;
import org.apache.sling.ide.transport.Command;
import org.apache.sling.ide.transport.CommandContext;
import org.apache.sling.ide.transport.FileInfo;
import org.apache.sling.ide.transport.Repository;
import org.apache.sling.ide.transport.RepositoryException;
import org.apache.sling.ide.transport.ResourceAndInfo;
import org.apache.sling.ide.transport.ResourceProxy;
import org.apache.sling.ide.util.PathUtil;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service = SyncCommandFactory.class)
public class DefaultSyncCommandFactory implements SyncCommandFactory {
    
    @Reference
    private SerializationManager serializationManager;
    
    @Reference
    private Logger logger;

    public DefaultSyncCommandFactory() {
    }

    public DefaultSyncCommandFactory(SerializationManager serializationManager, Logger logger) {
        setSerializationManager(serializationManager);
        setLogger(logger);
    }

    public DefaultSyncCommandFactory setSerializationManager(SerializationManager serializationManager) {
        this.serializationManager = serializationManager;
        return this;
    }

    public DefaultSyncCommandFactory setLogger(Logger logger) {
        this.logger = logger;
        return this;
    }

    @Override
    public Command<?> newCommandForRemovedResource(Repository repository, WorkspaceResource resource) throws IOException {
        
        if (resource.isIgnored()) {
            logger.trace("Skipping ignored resource {0}", resource);
            return null;
        }
        
        String repositoryPath = resource.getPathRelativeToSyncDir().absolute().asPortableString();

        FilterResult filterResult = resource.getProject().getFilter().filter(repositoryPath);
        
        logger.trace("Filter result for {0} is {1}", repositoryPath, filterResult);
        
        if (filterResult == null || filterResult == FilterResult.DENY || filterResult == FilterResult.PREREQUISITE) {
            return null;
        }
        
        // TODO - we send different parametrs to findSerializationDataFromCoveringParent 
        // for added vs removed resources
        
        // verify whether a resource being deleted does not signal that the content structure
        // was rearranged under a covering parent aggregate
        Path serializationFilePath = Paths.get(serializationManager.getSerializationFilePath(repositoryPath,
                SerializationKind.FOLDER));
        
        WorkspacePath serializationFileLocalPath = resource.getProject().getSyncDirectory().getLocalPath().append(WorkspacePaths.fromOsPath(serializationFilePath));

        ResourceProxy coveringParentData = findSerializationDataFromCoveringParent(resource, repositoryPath, serializationFileLocalPath);
        if (coveringParentData != null) {
            logger.trace("Found covering resource data ( repository path = {0} ) for resource at {1},  skipping deletion and performing an update instead",
                            coveringParentData.getPath(), resource.getLocalPath());
            FileInfo info = createFileInfo(resource);
            return repository.newAddOrUpdateNodeCommand(new CommandContext(resource.getProject().getFilter()), info, coveringParentData);
        }
        
        return repository.newDeleteNodeCommand(serializationManager.getRepositoryPath(repositoryPath));
    }
    
    @Override
    public Command<?> newCommandForAddedOrUpdatedResource(Repository repository, WorkspaceResource resource)
            throws IOException {
        ResourceAndInfo rai = buildResourceAndInfo(resource, repository);
        
        if (rai == null) {
            return null;
        }
        
        CommandContext context = new CommandContext(resource.getProject().getFilter());

        if (rai.isOnlyWhenMissing()) {
            return repository.newAddOrUpdateNodeCommand(context, rai.getInfo(), rai.getResource(),
                    Repository.CommandExecutionFlag.CREATE_ONLY_WHEN_MISSING);
        }

        return repository.newAddOrUpdateNodeCommand(context, rai.getInfo(), rai.getResource());
    }

   private ResourceProxy findSerializationDataFromCoveringParent(WorkspaceResource localFile,
           String resourceLocation, WorkspacePath serializationFilePath) throws IOException {

       logger.trace("Found plain nt:folder candidate at {0}, trying to find a covering resource for it",
localFile);
       
       WorkspacePath orig = serializationFilePath;

       WorkspacePath syncDirPath = localFile.getProject().getSyncDirectory().getLocalPath();
       serializationFilePath = localFile.getProject().getLocalPath().relativize(serializationFilePath);
       if ( serializationFilePath == null )
           throw new RuntimeException("Unable to get relative path from " + localFile.getProject().getLocalPath() + " to " + orig);
       serializationFilePath = serializationFilePath.absolute();
       
       while (!syncDirPath.equals(serializationFilePath)) {
           serializationFilePath = serializationFilePath.getParent();
           // TODO - better check for path going outside the sync directory
           if ( serializationFilePath.asPortableString().lastIndexOf('/') == 0 ) {
               break;
           }
           WorkspaceDirectory folderWithPossibleSerializationFile = localFile.getProject().getDirectory(serializationFilePath);
           
           
           if (!folderWithPossibleSerializationFile.exists()) {
               logger.trace("No folder found at {0}, moving up to the next level", serializationFilePath);
               continue;
           }

           // it's safe to use a specific SerializationKind since this scenario is only valid for METADATA_PARTIAL
           // coverage
           String possibleSerializationFilePath = serializationManager.getSerializationFilePath(
                   folderWithPossibleSerializationFile.getOSPath().toString(),
                   SerializationKind.METADATA_PARTIAL);

           logger.trace("Looking for serialization data in {0}", possibleSerializationFilePath);
           if (serializationManager.isSerializationFile(possibleSerializationFilePath)) {
               
               Path parentFileSerializationOSPath = localFile.getProject().getSyncDirectory().getOSPath().
                       relativize(Paths.get(possibleSerializationFilePath));

               WorkspacePath parentSerializationFilePath = new WorkspacePath(parentFileSerializationOSPath.toString())
                       .absolute();
               
               WorkspaceFile possibleSerializationFile = localFile.getProject().getSyncDirectory().getFile(parentSerializationFilePath);
               if (!possibleSerializationFile.exists()) {
                   logger.trace("Potential serialization data file {0} does not exist, moving up to the next level",
                           possibleSerializationFile.getLocalPath());
                   continue;
               }
               
               ResourceProxy serializationData;
               try (InputStream contents = possibleSerializationFile.getContents()) {
                   serializationData = serializationManager.readSerializationData(
                           parentSerializationFilePath.asPortableString(), contents);
               }

               String repositoryPath = serializationManager.getRepositoryPath(resourceLocation);
               String potentialPath = serializationData.getPath();
               boolean covered = serializationData.covers(repositoryPath);

               logger.trace(
                       "Found possible serialization data at {0}. Resource :{1} ; our resource: {2}. Covered: {3}",
                       parentSerializationFilePath, potentialPath, repositoryPath, covered);
               // note what we don't need to normalize the children here since this resource's data is covered by
               // another resource
               if (covered) {
                   return serializationData.getChild(repositoryPath);
               }

               break;
           }
       }

       return null;
   }

   private FileInfo createFileInfo(WorkspaceResource resource) {

       if (!(resource instanceof WorkspaceFile)) {
           return null;
       }

       FileInfo info = new FileInfo(resource.getOSPath().toString(), resource.getPathRelativeToSyncDir().asPortableString(), resource.getName());

       logger.trace("For {0} built fileInfo {1}", resource, info);

       return info;
   }
   
   public ResourceAndInfo buildResourceAndInfo(WorkspaceResource resource, Repository repository) throws IOException {

       if ( !resource.exists() ) {
           return null;
       }

       if ( resource.isIgnored()) {
           logger.trace("Skipping team-private resource {0}", resource);
           return null;
       }

       Long modificationTimestamp = (Long) resource.getTransientProperty(PN_IMPORT_MODIFICATION_TIMESTAMP);

       if (modificationTimestamp != null && modificationTimestamp >= resource.getLastModified()) {
           logger.trace("Change for resource {0} ignored as the import timestamp {1} >= modification timestamp {2}",
                           resource, modificationTimestamp, resource.getLastModified());
           return null;
       }

       FileInfo info = createFileInfo(resource);
       logger.trace("For {0} built fileInfo {1}", resource, info);

       WorkspaceDirectory syncDirectory = resource.getProject().getSyncDirectory();
       File syncDirectoryAsFile = syncDirectory.getOSPath().toFile();

       ResourceProxy resourceProxy = null;

       if (serializationManager.isSerializationFile(resource.getOSPath().toString())) {
           WorkspaceFile file = (WorkspaceFile) resource;
           try (InputStream contents = file.getContents()) {
               String resourceLocation = file.getPathRelativeToSyncDir().asPortableString();
               resourceProxy = serializationManager.readSerializationData(resourceLocation, contents);
               normaliseResourceChildren(file, resourceProxy, repository);


               // TODO - not sure if this 100% correct, but we definitely should not refer to the FileInfo as the
               // .serialization file, since for nt:file/nt:resource nodes this will overwrite the file contents
               String primaryType = (String) resourceProxy.getProperties().get(Repository.JCR_PRIMARY_TYPE);
               if (Repository.NT_FILE.equals(primaryType)) {
                   // TODO move logic to serializationManager
                   File locationFile = new File(info.getLocation());
                   String locationFileParent = locationFile.getParent();
                   int endIndex = locationFileParent.length() - ".dir".length();
                   File actualFile = new File(locationFileParent.substring(0, endIndex));
                   String newLocation = actualFile.getAbsolutePath();
                   String newName = actualFile.getName();
                   String newRelativeLocation = actualFile.getAbsolutePath().substring(
                           syncDirectoryAsFile.getAbsolutePath().length());
                   info = new FileInfo(newLocation, newRelativeLocation, newName);

                   logger.trace("Adjusted original location from {0} to {1}", resourceLocation, newLocation);

               }
           }
       } else {

           // TODO - move logic to serializationManager
           // possible .dir serialization holder
           if (resource instanceof WorkspaceDirectory && resource.getName().endsWith(".dir")) {
               WorkspaceDirectory folder = (WorkspaceDirectory) resource;
               WorkspaceResource contentXml = folder.getFile(new WorkspacePath(".content.xml"));
               // .dir serialization holder ; nothing to process here, the .content.xml will trigger the actual work
               if (contentXml.exists()
                       && serializationManager.isSerializationFile(contentXml.getOSPath().toString())) {
                   return null;
               }
           }

           resourceProxy = buildResourceProxyForPlainFileOrFolder(resource, repository);
       }
       
       if ( resourceProxy == null )
           throw new RuntimeException("ResourceProxy is null for resource " + resource);

       FilterResult filterResult = resource.getProject().getFilter().filter(resourceProxy.getPath());

       switch (filterResult) {

           case ALLOW:
               return new ResourceAndInfo(resourceProxy, info);
           case PREREQUISITE:
               // never try to 'create' the root node, we assume it exists
               if (!resourceProxy.getPath().equals("/")) {
                   // we don't explicitly set the primary type, which will allow the the repository to choose the best
                   // suited one ( typically nt:unstructured )
                   return new ResourceAndInfo(new ResourceProxy(resourceProxy.getPath()), null, true);
               }
           case DENY: // falls through
           default:
               return null;
       }
   }
   
   private ResourceProxy buildResourceProxyForPlainFileOrFolder(WorkspaceResource changedResource, Repository repository)
           throws IOException {

       SerializationKind serializationKind;
       String fallbackNodeType;
       if (changedResource instanceof WorkspaceFile) {
           serializationKind = SerializationKind.FILE;
           fallbackNodeType = Repository.NT_FILE;
       } else { // i.e. LocalFolder
           serializationKind = SerializationKind.FOLDER;
           fallbackNodeType = Repository.NT_FOLDER;
       }

       String resourceLocation = changedResource.getPathRelativeToSyncDir().asPortableString();
       String serializationFilePath = serializationManager.getSerializationFilePath(
               resourceLocation, serializationKind);
       WorkspaceFile serializationResource = changedResource.getProject().getSyncDirectory().getFile(new WorkspacePath(serializationFilePath));

       if (!serializationResource.exists() && changedResource instanceof WorkspaceDirectory) {
           ResourceProxy dataFromCoveringParent = findSerializationDataFromCoveringParent(changedResource,
                   resourceLocation, serializationResource.getLocalPath());

           if (dataFromCoveringParent != null) {
               return dataFromCoveringParent;
           }
           
       }

       return buildResourceProxy(resourceLocation, serializationResource, fallbackNodeType, repository);
   }
   
   private ResourceProxy buildResourceProxy(String resourceLocation, WorkspaceFile serializationFile,
           String fallbackPrimaryType, Repository repository) throws IOException {
       if (serializationFile.exists()) {
           try (InputStream contents = serializationFile.getContents() ) {
               
               String serializationFilePath = serializationFile.getPathRelativeToSyncDir().asPortableString();
               ResourceProxy resourceProxy = serializationManager.readSerializationData(serializationFilePath, contents);
               normaliseResourceChildren(serializationFile, resourceProxy, repository);

               return resourceProxy;
           }
       }

       return new ResourceProxy(serializationManager.getRepositoryPath(resourceLocation), Collections.singletonMap(
               Repository.JCR_PRIMARY_TYPE, (Object) fallbackPrimaryType));
   }
   
   /**
    * Normalises the of the specified <tt>resourceProxy</tt> by comparing the serialization data and the filesystem
    * data
    * 
    * @param serializationFile the file which contains the serialization data
    * @param resourceProxy the resource proxy
    * @param syncDirectory the sync directory
    * @param repository TODO
    * @throws CoreException
    */
   private void normaliseResourceChildren(WorkspaceFile serializationFile, ResourceProxy resourceProxy,
           Repository repository) {
       // TODO - this logic should be moved to the serializationManager
       try {
           SerializationKindManager skm = new SerializationKindManager();
           skm.init(repository);

           String primaryType = (String) resourceProxy.getProperties().get(Repository.JCR_PRIMARY_TYPE);
           List<String> mixinTypesList = getMixinTypes(resourceProxy);
           SerializationKind serializationKind = skm.getSerializationKind(primaryType, mixinTypesList);

           if (serializationKind == SerializationKind.METADATA_FULL) {
               return;
           }
       } catch (RepositoryException e) {
           // TODO proper exception handling
           throw new RuntimeException(e);
       }

       WorkspacePath serializationDirectoryPath = serializationFile.getLocalPath().getParent();

       Iterator<ResourceProxy> childIterator = resourceProxy.getChildren().iterator();
       Map<String, WorkspaceResource> extraChildResources = new HashMap<>();
       for (WorkspaceResource member : serializationFile.getParent().getChildren()) {
           if (member.equals(serializationFile)) {
               continue;
           }
           extraChildResources.put(member.getName(), member);
       }

       while (childIterator.hasNext()) {
           ResourceProxy child = childIterator.next();
           String childName = PathUtil.getName(child.getPath());
           String osChildName = serializationManager.getOsPath(childName);

           // covered children might have a FS representation, depending on their child nodes, so
           // accept a directory which maps to their name
           extraChildResources.remove(osChildName);

           // covered children do not need a filesystem representation
           if (resourceProxy.covers(child.getPath())) {
               continue;
           }

           WorkspacePath childPath = serializationDirectoryPath.append(osChildName);

           WorkspaceResource childResource = serializationFile.getParent().getDirectory(new WorkspacePath(osChildName));
           if (!childResource.exists()) {
               logger.trace("For resource at with serialization data {0} the serialized child resource at {1} does not exist in the filesystem and will be ignored",
                               serializationFile, childPath);
               childIterator.remove();
           }
       }

       for ( WorkspaceResource extraChildResource : extraChildResources.values()) {
           WorkspacePath extraChildResourcePath = extraChildResource.getPathRelativeToSyncDir();
           resourceProxy.addChild(new ResourceProxy(serializationManager
                   .getRepositoryPath(extraChildResourcePath.asPortableString())));
           
           logger.trace("For resource at with serialization data {0} the found a child resource at {1} which is not listed in the serialized child resources and will be added",
                           serializationFile, extraChildResource);
       }
   }
   
   private List<String> getMixinTypes(ResourceProxy resourceProxy) {

       Object mixinTypesProp = resourceProxy.getProperties().get(Repository.JCR_MIXIN_TYPES);

       if (mixinTypesProp == null) {
           return Collections.emptyList();
       }

       if (mixinTypesProp instanceof String) {
           return Collections.singletonList((String) mixinTypesProp);
       }

       return Arrays.asList((String[]) mixinTypesProp);
   }
   
    @Override
    public Command<Void> newReorderChildNodesCommand(Repository repository, WorkspaceResource resource) throws IOException {
        ResourceAndInfo rai = buildResourceAndInfo(resource, repository);

        if (rai == null || rai.isOnlyWhenMissing()) {
            return null;
        }

        return repository.newReorderChildNodesCommand(rai.getResource());
    }
}

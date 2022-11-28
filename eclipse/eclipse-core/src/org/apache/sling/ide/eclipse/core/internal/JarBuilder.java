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
package org.apache.sling.ide.eclipse.core.internal;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.osgi.framework.Constants;

/**
 * The <tt>JarBuilder</tt> creates a Jar file from a directory structure using the Eclipse APIs
 * 
 */
public class JarBuilder {

	private final IFolder sourceDir;
	private final Manifest manifest;

	public JarBuilder(final IFolder sourceDir) throws CoreException {
		this.sourceDir = sourceDir;
		
		IResource manifestResource = sourceDir.findMember(JarFile.MANIFEST_NAME);
        if (manifestResource == null || manifestResource.getType() != IResource.FILE) {
            throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "No file named "
                    + JarFile.MANIFEST_NAME + " found under " + sourceDir));
        }
        try (InputStream manifestInput = ((IFile) manifestResource).getContents()) {
        	 manifest = new Manifest(manifestInput);
        } catch (IOException e) {
            throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, e.getMessage(), e));
        }
	}
	
	public String getBundleSymbolicName() throws CoreException {
		String bsn = manifest.getMainAttributes().getValue(Constants.BUNDLE_SYMBOLICNAME);
		if (bsn == null) {
			throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "No Bundle-SymbolicName set in"
                    + JarFile.MANIFEST_NAME + " found under " + sourceDir));
		}
		return bsn;
	}

	/**
	 * Builds an in-memory JAR input stream from the files in the sourceDir.
	 * @return the input stream
	 * @throws CoreException
	 */
    public InputStream buildJar() throws CoreException {

        ByteArrayOutputStream store = new ByteArrayOutputStream();
        try (JarOutputStream zos = new JarOutputStream(store)) {
            zos.setLevel(Deflater.NO_COMPRESSION);
            // manifest first
            final ZipEntry anEntry = new ZipEntry(JarFile.MANIFEST_NAME);
            zos.putNextEntry(anEntry);
            manifest.write(zos);
            zos.closeEntry();
            zipDir(sourceDir, zos, "");
        } catch (IOException e) {
            throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, e.getMessage(), e));
        }
        return new ByteArrayInputStream(store.toByteArray());
    }

    private void zipDir(final IFolder sourceDir, final ZipOutputStream zos, final String path) throws CoreException,
            IOException {

        for (final IResource f : sourceDir.members()) {
            if (f.getType() == IResource.FOLDER) {
                final String prefix = path + f.getName() + "/";
                zos.putNextEntry(new ZipEntry(prefix));
                zipDir((IFolder) f, zos, prefix);
            } else if (f.getType() == IResource.FILE) {
                final String entry = path + f.getName();
                if (JarFile.MANIFEST_NAME.equals(entry)) {
                    continue;
                }
                try ( InputStream fis = ((IFile) f).getContents() ) {
                    final byte[] readBuffer = new byte[8192];
                    int bytesIn = 0;
                    final ZipEntry anEntry = new ZipEntry(entry);
                    zos.putNextEntry(anEntry);
                    while ((bytesIn = fis.read(readBuffer)) != -1) {
                        zos.write(readBuffer, 0, bytesIn);
                    }
                }
            }
        }
    }
}

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
package org.apache.sling.ide.cli.impl;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.WatchEvent.Kind;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;


// due to https://bugs.openjdk.java.net/browse/JDK-7133447 we need a long timeout on Mac OS
@Timeout(value = 30, unit=TimeUnit.SECONDS)
public class DirWatcherTest {

    @TempDir
    Path watchRoot;

    @Test
    public void addedFileInRoot() throws IOException, InterruptedException {
        try ( DirWatcher w = new DirWatcher(watchRoot) ) {
            Path newFile = Files.createFile(watchRoot.resolve("README"));
            DirWatcher.Event event = pollTillEventKind(w, ENTRY_CREATE);
            
            Assertions.assertEquals(watchRoot.relativize(newFile), event.getPath(), "event.path");
            Assertions.assertEquals(0, w.queueSize(), "queue.size");
        }
    }
    
    @Test
    public void addedFileInSubdir() throws IOException, InterruptedException {
        Path subDir = Files.createDirectory(watchRoot.resolve("subDir"));
        
        try ( DirWatcher w = new DirWatcher(watchRoot) ) {
            Path newFile = Files.createFile(subDir.resolve("README"));
    
            DirWatcher.Event event = pollTillEventKind(w, ENTRY_CREATE);
            Assertions.assertEquals(watchRoot.relativize(newFile), event.getPath(), "event.path");
        }

    }

    @Test
    public void addedFileInNewSubdir() throws IOException, InterruptedException {

        try ( DirWatcher w = new DirWatcher(watchRoot) ) {
    
            Path subDir = Files.createDirectory(watchRoot.resolve("subDir"));
    
            DirWatcher.Event event = pollTillEventKind(w, ENTRY_CREATE);
            Assertions.assertEquals(watchRoot.relativize(subDir), event.getPath(), "event.path");
            
            Path newFile = Files.createFile(subDir.resolve("README"));
            
            event = pollTillEventKind(w, ENTRY_CREATE);
            Assertions.assertEquals(watchRoot.relativize(newFile), event.getPath(), "event.path");
        }
    }

    @Test
    public void deletedFile() throws IOException, InterruptedException {
        Path subDir = Files.createDirectory(watchRoot.resolve("subDir"));
        Path newFile = Files.createFile(subDir.resolve("README"));

        try ( DirWatcher w = new DirWatcher(watchRoot) ) { 
            Files.delete(newFile);
            
            DirWatcher.Event event = pollTillEventKind(w, ENTRY_DELETE);
            Assertions.assertEquals(watchRoot.relativize(newFile), event.getPath(), "event.path");
        }
    }

    @Test
    public void deleteDir() throws IOException, InterruptedException {
        Path subDir = Files.createDirectory(watchRoot.resolve("subDir"));

        try ( DirWatcher w = new DirWatcher(watchRoot) ) { 
            
            Files.delete(subDir);
            
            DirWatcher.Event event = pollTillEventKind(w, ENTRY_DELETE);
        
            Assertions.assertEquals(watchRoot.relativize(subDir), event.getPath(), "event.path");
        }
    }

    public void modifyFile() throws IOException, InterruptedException {
        Path newFile = Files.createFile(watchRoot.resolve("README"));
        
        try ( DirWatcher w = new DirWatcher(watchRoot) ) { 
            
            Files.write(newFile, "hello, world".getBytes(StandardCharsets.UTF_8));
            DirWatcher.Event event = pollTillEventKind(w, ENTRY_MODIFY);
            Assertions.assertEquals(watchRoot.relativize(newFile), event.getPath(), "event.path");
            
            Files.write(newFile, "hello, world".getBytes(StandardCharsets.UTF_8));
            event = pollTillEventKind(w, ENTRY_MODIFY);
            Assertions.assertEquals(watchRoot.relativize(newFile), event.getPath(), "event.path");
        }
    }

    private DirWatcher.Event pollTillEventKind(DirWatcher watcher, Kind<?> kind) throws InterruptedException {
        DirWatcher.Event event;
        do {
            event = watcher.poll();
        } while(event.getKind() != kind);
        return event;
    }
}

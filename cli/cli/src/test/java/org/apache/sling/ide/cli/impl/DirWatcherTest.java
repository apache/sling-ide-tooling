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

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeFalse;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.apache.commons.lang3.SystemUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class DirWatcherTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();
    
    @Test(timeout = 3000)
    
    public void addedFileInRoot() throws IOException, InterruptedException {
    	// TODO: does not work on Mac OS yet
    	assumeFalse(SystemUtils.IS_OS_MAC);
        File watchRoot = folder.newFolder();
        
        try ( DirWatcher w = new DirWatcher(watchRoot.toPath()) ) {
            
            final File created = new File(watchRoot, "README");
            created.createNewFile();
            
            DirWatcher.Event event = w.poll();
            
            assertThat("event.kind", event.getKind(), equalTo(ENTRY_CREATE));
            assertThat("event.path", event.getPath(), equalTo(Paths.get(created.getName())));
            
            assertThat("queue.size", w.queueSize(), equalTo(0));
        }
    }
    
    @Test(timeout = 3000)
    public void addedFileInSubdir() throws IOException, InterruptedException {
    	// TODO: does not work on Mac OS yet
    	assumeFalse(SystemUtils.IS_OS_MAC);
        File watchRoot = folder.newFolder();
        File subDir = new File(watchRoot, "subDir");
        subDir.mkdir();
        
        try ( DirWatcher w = new DirWatcher(watchRoot.toPath()) ) {
            
            File created = new File(subDir, "README");
            created.createNewFile();
    
            DirWatcher.Event event = w.poll();
            
            assertThat("event.kind", event.getKind(), equalTo(ENTRY_CREATE));
            assertThat("event.path", event.getPath(), equalTo(Paths.get(subDir.getName(), created.getName())));
            
            assertThat("queue.size", w.queueSize(), equalTo(0));
        }

    }
    
    @Test(timeout = 3000)
    public void addedFileInNewSubdir() throws IOException, InterruptedException {
    	// TODO: does not work on Mac OS yet
    	assumeFalse(SystemUtils.IS_OS_MAC);
        File watchRoot = folder.newFolder();
        
        try ( DirWatcher w = new DirWatcher(watchRoot.toPath()) ) {
    
            File subDir = new File(watchRoot, "subDir");
            subDir.mkdir();
    
            DirWatcher.Event event = w.poll();
            
            assertThat("event.kind", event.getKind(), equalTo(ENTRY_CREATE));
            assertThat("event.path", event.getPath(), equalTo(Paths.get(subDir.getName())));
            
            File created = new File(subDir, "README");
            created.createNewFile();
            
            event = w.poll();
            assertThat("event.kind", event.getKind(), equalTo(ENTRY_CREATE));
            assertThat("event.path", event.getPath(), equalTo(Paths.get(subDir.getName(), created.getName())));
            
            assertThat("queue.size", w.queueSize(), equalTo(0));
        }
    }
    
    @Test(timeout = 3000)
    public void deletedFile() throws IOException, InterruptedException {
    	// TODO: does not work on Mac OS yet
    	assumeFalse(SystemUtils.IS_OS_MAC);
        assumeFalse(SystemUtils.IS_OS_WINDOWS); // TODO - SLING-7596
        
        File watchRoot = folder.newFolder();
        File subDir = new File(watchRoot, "subDir");
        subDir.mkdir();

        File created = new File(subDir, "README");
        created.createNewFile();

        try ( DirWatcher w = new DirWatcher(watchRoot.toPath()) ) { 

            created.delete();
            
            DirWatcher.Event event = w.poll();
        
            assertThat("event.kind", event.getKind(), equalTo(ENTRY_DELETE));
            assertThat("event.path", event.getPath(), equalTo(Paths.get(subDir.getName(), created.getName())));
            
            assertThat("queue.size", w.queueSize(), equalTo(0));
        }
    }
    
    @Test(timeout = 300000)
    public void deleteDir() throws IOException, InterruptedException {
    	// TODO: does not work on Mac OS yet
    	assumeFalse(SystemUtils.IS_OS_MAC);

        File watchRoot = folder.newFolder();
        File subDir = new File(watchRoot, "subDir");
        subDir.mkdir();


        try ( DirWatcher w = new DirWatcher(watchRoot.toPath()) ) { 
            
            Files.delete(subDir.toPath());
            
            DirWatcher.Event event = w.poll();
        
            assertThat("event.kind", event.getKind(), equalTo(ENTRY_DELETE));
            assertThat("event.path", event.getPath(), equalTo(Paths.get(subDir.getName())));
            
            assertThat("queue.size", w.queueSize(), equalTo(0));
        }
    }

    @Test(timeout = 3000)
    public void modifyFile() throws IOException, InterruptedException {
        // TODO: does not work on Mac OS yet
    	assumeFalse(SystemUtils.IS_OS_MAC);
    	
        File watchRoot = folder.newFolder();
        final File created = new File(watchRoot, "README");
        created.createNewFile();        
        
        try ( DirWatcher w = new DirWatcher(watchRoot.toPath()) ) { 
            
            Files.write(created.toPath(), "hello, world".getBytes(UTF_8));
            
            drainAndCheck(w, (events) -> {
                assertThat("events.size", events.size(), greaterThanOrEqualTo(1));
                for ( DirWatcher.Event event : events ) {
                    assertThat("event.kind", event.getKind(), equalTo(ENTRY_MODIFY));
                    assertThat("event.path", event.getPath(), equalTo(Paths.get(created.getName())));
                }
            });
            
            Files.write(created.toPath(), "hello, again".getBytes(UTF_8));
            
            drainAndCheck(w, (events) -> {
                assertThat("events.size", events.size(), greaterThanOrEqualTo(1));
                for ( DirWatcher.Event event : events ) {
                    assertThat("event.kind", event.getKind(), equalTo(ENTRY_MODIFY));
                    assertThat("event.path", event.getPath(), equalTo(Paths.get(created.getName())));
                }
            });
            
            List<DirWatcher.Event> unexpected = new ArrayList<>();
            while( w.queueSize() != 0 )
                unexpected.add(w.poll());
            
            // don't use size comparison to print out unexpected events in case of an assertion failure
            assertThat("unexpected events", unexpected, equalTo(new ArrayList<>()));
        }
    }
    
    private void drainAndCheck(DirWatcher w, Consumer<List<DirWatcher.Event>> check) throws InterruptedException {
        
        long start = System.currentTimeMillis();
        long delay = 500l;
        
        List<DirWatcher.Event> events = new ArrayList<>();
        while( System.currentTimeMillis() < start + delay) {
            if ( w.queueSize() == 0 ) {
                Thread.sleep(50);
                continue;
            }
            events.add(w.poll());
        }
        
        check.accept(events);
    }
}

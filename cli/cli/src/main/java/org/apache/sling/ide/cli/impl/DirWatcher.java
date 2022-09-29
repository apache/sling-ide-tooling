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
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Watches a whole directory tree for changes
 * 
 * <p>This class works on top of the standard  {@link WatchService} API by generating
 * events for all changes below a given directory.</p>
 * 
 * <p>This implementation works within the same constraints as the {@link WatchService} so
 * consumers are advised to read the documentation, particularly those related to platform limitations.
 * It is recommended to allow for minimal 
 */
public class DirWatcher implements AutoCloseable {
    
    private final Path root;
    private final WatchService ws;
    private final DualMap watched = new DualMap();
    private final Thread poller;
    private final BlockingQueue<DirWatcher.Event> queue = new LinkedBlockingQueue<>();

    public DirWatcher(Path path) throws IOException {
        this.root = path;
        ws = path.getFileSystem().newWatchService();

        poller = new Thread(() ->  {
            while ( !Thread.currentThread().isInterrupted() ) {
                try {
                    queue.addAll(pollInternal());
                } catch ( InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch ( ClosedWatchServiceException e) {
                    break;
                }
            }
        }, getClass().getSimpleName() +"-Poller");
        
        Stream.concat(
            Stream.of(root),
            Files.walk(root).filter(p -> p.toFile().isDirectory())
        ).forEach( this::register);
        
        poller.start();
    }
    
    public void close() throws IOException {
        if ( poller != null )
            poller.interrupt();
        if ( ws != null)
            ws.close();
    }

    /**
     * Takes a single event from the queue, blocking if none are available
     * 
     * @return the event
     * @throws InterruptedException interrupted
     */
    public Event poll() throws InterruptedException {
        return queue.take();
    }
    
    // visible for testing
    int queueSize() {
        return queue.size();
    }
    
    private void register(Path path) {
        try {
            WatchKey key = path.register(ws, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
            watched.put(key, path);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void unregister(Path path) {
        WatchKey key = watched.remove(path);
        if ( key != null )
            key.cancel();
        
    }
    
    private List<DirWatcher.Event> pollInternal() throws InterruptedException {
        final WatchKey key = ws.take();
        
        List<DirWatcher.Event> result = key.pollEvents().stream()
            .filter( e -> e.context() instanceof Path )
            .map( Event::new )
            .map( e -> updateTracked(e, key) )
            .map( e -> adjust(e, key) )
            .collect( Collectors.toList() );
        
        key.reset();
        
        return result;
    }

    private DirWatcher.Event adjust(Event e, WatchKey key) {
        Path keyPath = watched.get(key);
        e.path = root.relativize(keyPath.resolve(e.path));
        return e;
    }
    
    private DirWatcher.Event updateTracked(DirWatcher.Event evt, WatchKey key) {
        if ( evt.getKind() == StandardWatchEventKinds.ENTRY_CREATE ) {
            Path fullPath = watched.get(key).resolve(evt.getPath());
            if ( fullPath.toFile().isDirectory())
                register(fullPath);            
        } else if ( evt.getKind() == StandardWatchEventKinds.ENTRY_DELETE ) {
            Path fullPath = watched.get(key).resolve(evt.getPath());
            // we can't check if the path pointed to a directory since it is already deleted
            unregister(fullPath);
        }

        return evt;
    }
    
    public static class Event {
        
        public Event(WatchEvent<?> wrapper) {
            kind = wrapper.kind();
            path = (Path) wrapper.context();
            count = wrapper.count();
        }
        
        private Kind<?> kind;
        private Path path;
        private int count;
        
        public Kind<?> getKind() {
            return kind;
        }
        
        public Path getPath() {
            return path;
        }
        
        @Override
        public String toString() {
            return getClass().getSimpleName() + " [kind: " + kind +", path: " + path + ", count: " + count +"]";
        }
    }
    
    static class DualMap {

        private final Map<WatchKey, Path> forward = new HashMap<>();
        private final Map<Path, WatchKey> reverse = new HashMap<>();
        private final Object sync = new Object();
        
        public void put(WatchKey key, Path path) {
            synchronized (sync) {
                forward.put(key, path);
                reverse.put(path, key);
            }
        }
        
        public Path get(WatchKey key) {
            synchronized (sync) {
                return forward.get(key);
            }
        }
        
        public WatchKey get(Path path) {
            synchronized (sync) {
                return reverse.get(path);
            }
        }
        
        public WatchKey remove(Path path) {
            synchronized (sync) {
                WatchKey key = reverse.get(path);
                if ( key != null )
                    forward.remove(key);
                return key;
            }
        }
    }
}

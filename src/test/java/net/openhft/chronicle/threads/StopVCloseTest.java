/*
 * Copyright 2016-2022 chronicle.software
 *
 *       https://chronicle.software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.openhft.chronicle.threads;

import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.StackTrace;
import net.openhft.chronicle.core.threads.EventHandler;
import net.openhft.chronicle.core.threads.EventLoop;
import net.openhft.chronicle.core.threads.HandlerPriority;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class StopVCloseTest extends ThreadsTestCommon {

    @BeforeEach
    public void handlersInit() {
        ignoreException("Monitoring a task which has finished ");
        MonitorEventLoop.MONITOR_INITIAL_DELAY_MS = 1;
    }

    @Override
    public void preAfter() {
        MonitorEventLoop.MONITOR_INITIAL_DELAY_MS = 10_000;
    }

    @Test
    public void eventGroupStop() {
        final EnumSet<HandlerPriority> allPriorities = EnumSet.allOf(HandlerPriority.class);
        try (final EventLoop eventGroup = EventGroup.builder()
                .withConcurrentThreadsNum(1)
                .withPriorities(allPriorities)
                .build()) {
            eventGroup.start();

            Set<HandlerPriority> started = Collections.synchronizedSet(EnumSet.noneOf(HandlerPriority.class));
            Set<HandlerPriority> stopped = Collections.synchronizedSet(EnumSet.noneOf(HandlerPriority.class));
            for (HandlerPriority hp : allPriorities)
                eventGroup.addHandler(new EventHandler() {
                    @Override
                    public boolean action() {
                        return true;
                    }

                    @Override
                    public void loopStarted() {
                        started.add(hp);
                    }

                    @Override
                    public void loopFinished() {
                        stopped.add(hp);
                    }

                    @Override
                    public @NotNull HandlerPriority priority() {
                        return hp;
                    }
                });

            for (int i = 0; i < 100; i++)
                if (!started.contains(HandlerPriority.MONITOR))
                    Jvm.pause(1);
            eventGroup.stop();
            eventGroup.awaitTermination();
            assertTrue(eventGroup.isStopped());
            assertEquals(allPriorities, started);
            assertEquals(allPriorities, stopped);
        }
    }

    @Test
    public void blockingStopped() throws InterruptedException {
        BlockingEventLoop bel = new BlockingEventLoop("blocking");
        bel.start();
        BlockingQueue<String> q = new LinkedBlockingQueue<>();
        AtomicBoolean stopped = new AtomicBoolean();
        AtomicReference<Thread> thread = new AtomicReference<>();
        bel.addHandler(() -> {
            try {
                thread.set(Thread.currentThread());
                q.add("token");
                LockSupport.parkNanos(2_000_000_000L);
                return false;
            } finally {
                stopped.set(true);
            }
        });
        q.poll(1, TimeUnit.SECONDS);
        bel.close();
        if (thread.get().isAlive())
            StackTrace.forThread(thread.get()).printStackTrace();
        assertTrue(stopped.get());
    }
}

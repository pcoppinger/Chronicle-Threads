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

package net.openhft.chronicle.threads.internal;

import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.threads.EventLoop;
import net.openhft.chronicle.threads.AbstractLifecycleEventLoop;
import net.openhft.chronicle.threads.CoreEventLoop;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;

/**
 * This is a utility to render a verbose summary of the state of an {@link EventLoop}. Useful for debugging.
 */
public enum EventLoopStateRenderer {
    INSTANCE;

    public String render(String name, @Nullable EventLoop eventLoop) {
        if (eventLoop == null) {
            return name + " event loop is null";
        }
        StringBuilder builder = new StringBuilder();
        builder.append(name).append(" event loop state\n");
        builder.append("#toString(): ").append(eventLoop).append('\n');
        builder.append("Closed: ").append(eventLoop.isClosed()).append('\n');
        builder.append("Closing: ").append(eventLoop.isClosing()).append('\n');
        addLifecycleDetails(builder, eventLoop);
        addCoreEventLoopDetails(builder, eventLoop);
        return builder.toString();
    }

    private void addCoreEventLoopDetails(StringBuilder builder, EventLoop eventLoop) {
        if (eventLoop instanceof CoreEventLoop) {
            Thread t = ((CoreEventLoop) eventLoop).thread();
            if (t != null) {
                builder.append("Thread state: ").append(t.getState()).append('\n');
                final StackTraceElement[] stackTrace = t.getStackTrace();
                if (stackTrace.length > 0) {
                    builder.append("Stack trace:");
                    Jvm.trimStackTrace(builder, stackTrace);
                }
            } else {
                builder.append("Thread is null\n");
            }
        }
    }

    private void addLifecycleDetails(StringBuilder builder, EventLoop eventLoop) {
        if (eventLoop instanceof AbstractLifecycleEventLoop) {
            try {
                final Field lifecycle = Jvm.getField(eventLoop.getClass(), "lifecycle");
                builder.append("Lifecycle: ").append(lifecycle.get(eventLoop)).append('\n');
            } catch (IllegalAccessException e) {
                Jvm.warn().on(EventLoopStateRenderer.class, "Error getting the lifecycle for " + eventLoop.getClass().getName());
            }
        }
    }
}

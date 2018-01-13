/*
 * This file is part of OurTube-serverj, licensed under the MIT License (MIT).
 *
 * Copyright (c) Kenzie Togami (kenzierocks) <https://kenzierocks.me>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package me.kenzierocks.ourtube.guildqueue;

import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import me.kenzierocks.ourtube.Events;

public enum GuildQueue {
    INSTANCE;

    public final Events events = new Events("GuildQueue");

    private final Map<String, GQ> songQueues = new ConcurrentHashMap<>();

    public void useQueue(String guildId, Consumer<Deque<String>> user) {
        songQueues.computeIfAbsent(guildId, k -> new GQ()).useQueue(user);
    }

    public void queueSong(String guildId, String songId) {
        useQueue(guildId, q -> q.addLast(songId));
        events.post(guildId, PushSong.create(songId));
    }

    public void popSong(String guildId) {
        useQueue(guildId, q -> {
            if (q.isEmpty()) {
                return;
            }
            q.removeFirst();
            events.post(guildId, PopSong.create());
        });
    }

}

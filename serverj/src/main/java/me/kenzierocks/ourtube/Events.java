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
package me.kenzierocks.ourtube;

import java.util.HashMap;
import java.util.Map;

import com.google.common.eventbus.EventBus;

public class Events {

    private final String id;
    private final Map<String, EventBus> guildEventBuses = new HashMap<>();

    public Events(String id) {
        this.id = id;
    }

    public void subscribe(String guildId, Object subscription) {
        guildEventBuses.computeIfAbsent(guildId, k -> new EventBus(id + "-" + guildId)).register(subscription);
    }

    public void unsubscribe(String guildId, Object subscription) {
        EventBus bus = guildEventBuses.get(guildId);
        if (bus != null) {
            bus.unregister(subscription);
        }
    }

    public void post(String guildId, Object event) {
        EventBus bus = guildEventBuses.get(guildId);
        if (bus != null) {
            bus.post(event);
        }
    }

}

/*
 * This file is part of OurTube-serverj, licensed under the MIT License (MIT).
 *
 * Copyright (c) Octavia Togami (octylFractal) <https://octyl.net>
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

package me.kenzierocks.ourtube.rpc;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import com.google.common.eventbus.EventBus;

public class RpcRegistry {

    private final Map<String, RpcEventHandler.Typed<Object>> functions = new ConcurrentHashMap<>();
    private final EventBus events = new EventBus("rpc-registry");

    public EventBus getEvents() {
        return events;
    }

    public void register(String event, RpcEventHandler.Typed<?> function) {
        @SuppressWarnings("unchecked")
        RpcEventHandler.Typed<Object> ez = (RpcEventHandler.Typed<Object>) function;
        functions.put(event, ez);
    }

    public Optional<RpcEventHandler.Typed<Object>> get(String event) {
        return Optional.of(functions.get(event));
    }

    public Optional<Class<Object>> getArgumentClass(String event) {
        return get(event)
                .map(RpcEventHandler.Typed::argsClass);
    }

}

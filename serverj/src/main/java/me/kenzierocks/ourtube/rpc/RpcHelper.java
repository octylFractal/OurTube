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
package me.kenzierocks.ourtube.rpc;

import java.io.IOException;
import java.util.Optional;

import org.slf4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import me.kenzierocks.ourtube.Log;

public class RpcHelper {

    private static final Logger LOGGER = Log.get();

    private final RpcRegistry registry;
    private final ObjectMapper mapper;

    public RpcHelper(RpcRegistry registry, ObjectMapper mapper) {
        this.registry = registry;
        this.mapper = mapper;
    }

    public RpcRegistry getRegistry() {
        return registry;
    }

    public ObjectMapper getMapper() {
        return mapper;
    }

    public void callEvent(RpcClient client, String eventJson) throws IOException {
        JsonNode node = mapper.readTree(eventJson);
        JsonNode nameNode = node.get("name");
        if (nameNode == null) {
            return;
        }
        String name = nameNode.asText();
        Optional<Class<Object>> argClass = registry.getArgumentClass(name);
        if (!argClass.isPresent()) {
            LOGGER.debug("client={}, Ignoring event '{}': not registered", client.getId(), name);
            return;
        }
        Object args = mapper.treeToValue(node.get("arguments"), argClass.get());
        registry.get(name).get().handleEvent(client, args);
    }

}

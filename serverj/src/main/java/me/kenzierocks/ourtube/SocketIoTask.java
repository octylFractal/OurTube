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

import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.protocol.JacksonJsonSupport;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;

import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.FutureListener;

public class SocketIoTask implements Runnable {

    @Override
    public void run() {
        Configuration config = new Configuration();
        config.setPort(13445);
        config.setContext("/transport-layer");
        config.setJsonSupport(new JacksonJsonSupport(
                new Jdk8Module(),
                new GuavaModule()));
        config.getSocketConfig().setReuseAddress(true);
        SocketIOServer server = new SocketIOServer(config);
        new OurTubeApi(server.addNamespace("/api")).configure();
        server.startAsync().addListener(new FutureListener<Void>() {

            @Override
            public void operationComplete(Future<Void> future) throws Exception {
                if (!future.isSuccess()) {
                    System.exit(1);
                }
            }
        });
    }

}

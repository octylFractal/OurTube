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

import java.util.concurrent.Callable;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import org.slf4j.Logger;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import me.kenzierocks.ourtube.response.Response;
import me.kenzierocks.ourtube.rpc.RpcClient;

public class AsyncService {

    private static final Logger LOGGER = Log.get();

    public static final ListeningScheduledExecutorService GENERIC = MoreExecutors.listeningDecorator(
            new ScheduledThreadPoolExecutor(
                    50,
                    new ThreadFactoryBuilder().setDaemon(true).setNameFormat("generic-%d").build()));

    public static void asyncResponse(RpcClient client, String callbackName, Callable<? extends Object> response) {
        asyncResponse(client, callbackName, GENERIC.submit(response));
    }

    public static void asyncResponse(RpcClient client, String callbackName, ListenableFuture<? extends Object> future) {
        Futures.addCallback(Response.from(future),
                new FutureCallback<Response<? extends Object>>() {

                    @Override
                    public void onSuccess(Response<? extends Object> result) {
                        client.callFunction(callbackName, result);
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        LOGGER.error("Unexpected failure getting song data", t);
                    }
                },
                AsyncService.GENERIC);
    }

}

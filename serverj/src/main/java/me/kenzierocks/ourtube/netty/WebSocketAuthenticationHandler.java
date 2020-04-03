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

package me.kenzierocks.ourtube.netty;

import static com.google.common.collect.ImmutableListMultimap.toImmutableListMultimap;

import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.type.TypeFactory;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Maps;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler.HandshakeComplete;
import me.kenzierocks.ourtube.AuditLog;
import me.kenzierocks.ourtube.AuditLog.Action;
import me.kenzierocks.ourtube.rpc.RpcDisconnect;
import me.kenzierocks.ourtube.rpc.RpcHelper;
import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class WebSocketAuthenticationHandler extends ChannelDuplexHandler {

    private static final String TOKEN = "token";

    private final OkHttpClient http = new OkHttpClient.Builder()
            .connectionPool(new ConnectionPool(10, 10, TimeUnit.MINUTES))
            .build();
    private final RpcClientRegistry clients;
    private final RpcHelper helper;

    public WebSocketAuthenticationHandler(RpcClientRegistry clients, RpcHelper helper) {
        this.clients = clients;
        this.helper = helper;
    }

    @Override
    public void close(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
        helper.getRegistry().getEvents().post(RpcDisconnect.create(clients.getClient(ctx)));
        clients.remove(ctx.channel());
        super.close(ctx, promise);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof HandshakeComplete) {
            Action connect = AuditLog.action("WS-" + ctx.channel().id().asShortText(), "ws.connect")
                    .attempted();
            String uri = ((HandshakeComplete) evt).requestUri();
            ListMultimap<String, String> params = new QueryStringDecoder(uri).parameters()
                    .entrySet().stream()
                    .flatMap(e -> e.getValue().stream().map(v -> Maps.immutableEntry(e.getKey(), v)))
                    .collect(toImmutableListMultimap(Entry::getKey, Entry::getValue));
            if (!params.containsKey(TOKEN)) {
                // ignore request with bad authentication
                connect.denied();
                ctx.close();
                return;
            }
            String token = params.get(TOKEN).get(0);

            String userId = validateToken(token);
            if (userId == null) {
                // ignore request with bad authentication
                connect.denied();
                ctx.close();
                return;
            }

            clients.register(ctx.channel(), userId, token);
            connect.performed();
        }
        super.userEventTriggered(ctx, evt);
    }

    private String validateToken(String token) throws IOException {
        Response response = http.newCall(new Request.Builder()
                .url("https://discordapp.com/api/v6/users/@me")
                .header(HttpHeaderNames.AUTHORIZATION.toString(), "Bearer " + token)
                .build()).execute();
        Map<String, Object> userObj = helper.getMapper().readValue(response.body().string(), TypeFactory.unknownType());
        Object id = userObj.get("id");
        return id == null ? null : id.toString();
    }

}

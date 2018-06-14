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

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class DissyAuth {

    private static final Logger LOGGER = Log.get();
    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectionPool(new ConnectionPool(50, 10, TimeUnit.HOURS))
            .retryOnConnectionFailure(true)
            .build();

    private static final LoadingCache<String, Boolean> validityCache = CacheBuilder.newBuilder()
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .build(CacheLoader.from(DissyAuth::checkValidToken));

    public static boolean isValidToken(String token) {
        return validityCache.getUnchecked(token);
    }

    private static boolean checkValidToken(String token) {
        String validityCheckId = UUID.randomUUID().toString();
        for (int tries = 5; tries > 0; tries--) {
            try {
                Response res = client.newCall(new Request.Builder()
                        .url("https://discordapp.com/api/v6/users/@me")
                        .addHeader("Authentication", token)
                        .build())
                        .execute();
                return res.isSuccessful();
            } catch (IOException e) {
                LOGGER.error("[" + validityCheckId + "] Error retrieving token validity status", e);
            }
            if (tries > 0) {
                LOGGER.info("[" + validityCheckId + "] Retrying validity check...");
            }
        }
        return false;
    }

}

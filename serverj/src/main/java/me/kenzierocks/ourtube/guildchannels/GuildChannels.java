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

package me.kenzierocks.ourtube.guildchannels;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import me.kenzierocks.ourtube.AuditLog;
import me.kenzierocks.ourtube.Events;

public enum GuildChannels {
    INSTANCE;

    public final Events events = new Events("GuildChannels");

    private final Map<String, Optional<String>> channelsMap = new ConcurrentHashMap<>();

    @Nullable
    public String getChannel(String guildId) {
        Optional<String> val = channelsMap.get(guildId);
        return val == null ? null : val.orElse(null);
    }

    public void setChannel(String guildId, String userId, @Nullable String channelId) {
        AuditLog.action(userId, "guild(%s).setChannel(%s)", guildId, channelId)
                .attempted().performed();
        Optional<String> newId = Optional.ofNullable(channelId);
        Optional<String> old = channelsMap.put(guildId, newId);
        if (!Objects.equals(newId, old)) {
            events.post(guildId, NewChannel.create(channelId));
        }
    }

}

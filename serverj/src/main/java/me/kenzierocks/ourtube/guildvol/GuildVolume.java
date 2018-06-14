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
package me.kenzierocks.ourtube.guildvol;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import me.kenzierocks.ourtube.AuditLog;
import me.kenzierocks.ourtube.Events;

public enum GuildVolume {
    INSTANCE;

    public static final float DEFAULT_VOLUME = 30f;

    public final Events events = new Events("GuildVolume");

    private final Map<String, Float> volumeMap = new ConcurrentHashMap<>();

    public float getVolume(String guildId) {
        Float volume = volumeMap.get(guildId);
        return volume == null ? DEFAULT_VOLUME : volume;
    }

    public void setVolume(String guildId, String userId, float volume) {
        AuditLog.action(userId, "guild(%s).setVolume(%s)", guildId, volume)
                .attempted().performed();
        Float old = volumeMap.put(guildId, volume);
        if (!Objects.equals(volume, old)) {
            events.post(guildId, SetVolume.create(volume));
        }
    }

}

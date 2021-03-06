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

package me.kenzierocks.ourtube.guildqueue;

import static com.google.common.collect.ImmutableList.toImmutableList;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import com.sedmelluq.discord.lavaplayer.track.BaseAudioTrack;
import discord4j.core.object.util.Snowflake;
import org.slf4j.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import me.kenzierocks.ourtube.AuditLog;
import me.kenzierocks.ourtube.AuditLog.Action;
import me.kenzierocks.ourtube.Dissy;
import me.kenzierocks.ourtube.Events;
import me.kenzierocks.ourtube.Log;
import me.kenzierocks.ourtube.OurTube;
import me.kenzierocks.ourtube.YoutubeAccess;
import me.kenzierocks.ourtube.lava.OurTubeAudioTrack;
import me.kenzierocks.ourtube.lava.OurTubeItemInfo;
import me.kenzierocks.ourtube.lava.TrackScheduler;

public enum GuildQueue {
    INSTANCE;

    private static final Logger LOGGER = Log.get();

    public final Events events = new Events("GuildQueue");

    public void queueSongs(Snowflake guildId, Snowflake userId, String songUrl) {
        Action queueSongs = AuditLog.action(userId, "guild(%s).queueSongs(%s)", guildId, songUrl)
                .attempted();
        List<String> songIds = YoutubeAccess.INSTANCE.getSongIds(songUrl);
        List<CompletableFuture<AudioTrack>> allSongs =
                songIds.stream()
                        .map(id -> createTrack(id, userId))
                        .map(cf -> cf.handle((res, err) -> {
                            if (res != null) {
                                return res;
                            } else if (err != null) {
                                LOGGER.error("Error processing song ID " + songIds, err);
                            }
                            return null;
                        }))
                        .collect(toImmutableList());
        queueSongs.performed();

        TrackScheduler sch = Dissy.getScheduler(guildId);
        allSongs.forEach(song -> {
            AudioTrack track = Dissy.getCfEasily(song);
            if (track == null) {
                return;
            }
            AuditLog.action(userId, "guild(%s).addSong(%s)", guildId, AuditLog.songInfo(track.getIdentifier()))
                    .performed();
            sch.addTrack(userId, track);
        });
    }

    private CompletableFuture<AudioTrack> createTrack(String id, Snowflake submitter) {
        try {
            return Dissy.loadItem("ourtube:" + OurTube.MAPPER.writeValueAsString(
                    OurTubeItemInfo.create(id, submitter.asString())));
        } catch (JsonProcessingException e) {
            CompletableFuture<AudioTrack> future = new CompletableFuture<>();
            future.completeExceptionally(e);
            return future;
        }
    }

    public void skipSong(Snowflake guildId, Snowflake userId, String songId) {
        Action skipSong = AuditLog.action(userId, "guild(%s).skipSong(%s)", guildId, AuditLog.songInfo(songId))
                .attempted();

        AudioPlayer player = Dissy.getPlayer(guildId);
        AudioTrack latest = player.getPlayingTrack();
        String songIdSkipped = OurTubeAudioTrack.cast(latest)
            .map(BaseAudioTrack::getIdentifier)
            .orElse(null);

        if (latest == null || !Objects.equals(songId, songIdSkipped)) {
            skipSong.log("canceled");
            return;
        }

        skipSong.performed();

        Dissy.getScheduler(guildId).skipTrack();
    }

}

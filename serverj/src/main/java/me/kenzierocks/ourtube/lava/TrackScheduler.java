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
package me.kenzierocks.ourtube.lava;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;

import me.kenzierocks.ourtube.AsyncService;
import me.kenzierocks.ourtube.AudioUpdatesTask;
import me.kenzierocks.ourtube.Dissy;
import me.kenzierocks.ourtube.Log;
import me.kenzierocks.ourtube.guildqueue.GuildQueue;
import me.kenzierocks.ourtube.guildqueue.PopSong;
import me.kenzierocks.ourtube.guildqueue.PushSong;

public class TrackScheduler extends AudioEventAdapter {

    private static final Logger LOGGER = Log.get();

    private final String guildId;
    private final AudioPlayer player;
    private final BlockingQueue<AudioTrack> queue;
    private final Lock accessLock = new ReentrantLock();

    public TrackScheduler(String guildId, AudioPlayer player) {
        this.guildId = guildId;
        this.player = player;
        player.addListener(this);
        this.queue = new LinkedBlockingQueue<>();
    }

    public BlockingQueue<AudioTrack> getQueue() {
        return queue;
    }

    public Lock getAccessLock() {
        return accessLock;
    }

    public void addTrack(AudioTrack track) {
        accessLock.lock();
        try {
            LOGGER.debug("Queued track " + track.getIdentifier());
            queue.add(track);
            GuildQueue.getSongId(track)
                    .ifPresent(songId -> {
                        GuildQueue.INSTANCE.events.post(guildId, PushSong.create(songId));
                    });
            player.startTrack(track, true);
        } finally {
            accessLock.unlock();
        }
    }

    public void nextTrack() {
        accessLock.lock();
        try {
            AudioTrack nextTrack = queue.peek();
            if (nextTrack != null) {
                LOGGER.debug("Started track " + nextTrack.getIdentifier());
            } else {
                LOGGER.debug("Stopping current track, no replacement.");
            }
            player.startTrack(nextTrack, false);
        } finally {
            accessLock.unlock();
        }
    }

    public void skipTrack() {
        queue.poll();
        nextTrack();
    }

    @Override
    public void onTrackStart(AudioPlayer player, AudioTrack track) {
        GuildQueue.getSongId(track)
                .ifPresent(songId -> {
                    AsyncService.GENERIC.execute(new AudioUpdatesTask(
                            player,
                            Dissy.getProvider(guildId),
                            track,
                            guildId,
                            songId));
                });
    }

    @Override
    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
        accessLock.lock();
        try {
            GuildQueue.getSongId(track)
                    .ifPresent(songId -> {
                        GuildQueue.INSTANCE.events.post(guildId, PopSong.create(songId));
                    });
            if (endReason.mayStartNext) {
                queue.poll();
                nextTrack();
            }
        } finally {
            accessLock.unlock();
        }
    }
}

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

package me.kenzierocks.ourtube.lava;

import java.util.Collection;
import java.util.Comparator;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import com.sedmelluq.discord.lavaplayer.track.BaseAudioTrack;
import discord4j.core.object.util.Snowflake;
import discord4j.voice.VoiceConnection;
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

    private final Snowflake guildId;
    private final AudioPlayer player;
    private final ConcurrentHashMap<Snowflake, BlockingQueue<AudioTrack>> queue;
    private final Lock accessLock = new ReentrantLock();

    public TrackScheduler(Snowflake guildId, AudioPlayer player) {
        this.guildId = guildId;
        this.player = player;
        player.addListener(this);
        this.queue = new ConcurrentHashMap<>();
    }

    public Stream<AudioTrack> allTracksStream() {
        return queue.values().stream()
                .flatMap(Collection::stream)
                .sorted(OurTubeAudioTrack.CMP_QUEUE_TIME);
    }

    public BlockingQueue<AudioTrack> getQueue(Snowflake userId) {
        return queue.computeIfAbsent(userId, uid -> new LinkedBlockingDeque<>());
    }

    private Optional<BlockingQueue<AudioTrack>> peekNextQueue() {
        var connected = Dissy.getConnection(guildId);
        if (connected == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(connected.channel.getVoiceStates()
                .map(user -> getQueue(user.getUserId()))
                .filter(queue -> !queue.isEmpty())
                .sort(Comparator.comparing(BlockingQueue::peek, OurTubeAudioTrack.CMP_QUEUE_TIME))
                .blockFirst());
    }

    @Nullable
    private AudioTrack pollNextTrack() {
        accessLock.lock();
        try {
            return peekNextQueue().map(Queue::poll).orElse(null);
        } finally {
            accessLock.unlock();
        }
    }

    @Nullable
    private AudioTrack peekNextTrack() {
        accessLock.lock();
        try {
            return peekNextQueue().map(Queue::peek).orElse(null);
        } finally {
            accessLock.unlock();
        }
    }

    public Lock getAccessLock() {
        return accessLock;
    }

    public void addTrack(Snowflake userId, AudioTrack track) {
        accessLock.lock();
        try {
            LOGGER.debug("Queued track " + track.getIdentifier());
            getQueue(userId).add(track);
            OurTubeAudioTrack.cast(track)
                    .map(BaseAudioTrack::getIdentifier)
                    .ifPresent(songId -> {
                        String nick = Dissy.getNameForUserInGuild(guildId, userId);
                        GuildQueue.INSTANCE.events.post(guildId, PushSong.create(songId, nick));
                    });
            nextTrack(true);
        } finally {
            accessLock.unlock();
        }
    }

    public void nextTrack(boolean noInterrupt) {
        accessLock.lock();
        try {
            Optional<BlockingQueue<AudioTrack>> queue = peekNextQueue();
            AudioTrack nextTrack = queue.map(Queue::peek).orElse(null);
            if (nextTrack != null) {
                LOGGER.debug("Started track " + nextTrack.getIdentifier());
            } else {
                LOGGER.debug("Stopping current track, no replacement.");
            }
            if (player.startTrack(nextTrack, noInterrupt)) {
                queue.ifPresent(Queue::poll);
            }
        } finally {
            accessLock.unlock();
        }
    }

    public void skipTrack() {
        accessLock.lock();
        try {
            nextTrack(false);
        } finally {
            accessLock.unlock();
        }
    }

    @Override
    public void onTrackStart(AudioPlayer player, AudioTrack track) {
        OurTubeAudioTrack.cast(track)
                .map(BaseAudioTrack::getIdentifier)
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
            OurTubeAudioTrack.cast(track)
                    .map(otat -> otat.getIdentifier())
                    .ifPresent(songId -> {
                        GuildQueue.INSTANCE.events.post(guildId, PopSong.create(songId));
                    });
            if (endReason.mayStartNext) {
                nextTrack(false);
            }
        } finally {
            accessLock.unlock();
        }
    }

}

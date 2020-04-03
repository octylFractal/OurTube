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

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import me.kenzierocks.ourtube.lava.AudioProvider;
import me.kenzierocks.ourtube.songprogress.SongProgress;
import me.kenzierocks.ourtube.songprogress.SongProgressMap;

public class AudioUpdatesTask implements Runnable {

    private static final Logger LOGGER = Log.get();

    private final AudioPlayer player;
    private final AudioProvider timeTracker;
    private final AudioTrack track;
    private final String guildId;
    private final String songId;
    private final long initialMs;

    public AudioUpdatesTask(AudioPlayer player, AudioProvider timeTracker, AudioTrack track, String guildId, String songId) {
        this.player = player;
        this.timeTracker = timeTracker;
        this.track = track;
        this.guildId = guildId;
        this.songId = songId;
        this.initialMs = timeTracker.getDurationProvidedMs();
    }

    @Override
    public void run() {
        if (player.getPlayingTrack() != this.track) {
            LOGGER.debug("{}: stopped updating progress, track wasn't playing!", songId);
            return;
        }

        double progress = (100 * (timeTracker.getDurationProvidedMs() - initialMs)) / (double) track.getDuration();
        SongProgressMap.INSTANCE.setProgress(guildId, SongProgress.create(songId, progress));

        AsyncService.GENERIC.schedule(this, track.getDuration() / 200, TimeUnit.MILLISECONDS);
    }

}

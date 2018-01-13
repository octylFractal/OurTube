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

import me.kenzierocks.ourtube.songprogress.SongProgress;
import me.kenzierocks.ourtube.songprogress.SongProgressMap;
import sx.blah.discord.util.audio.AudioPlayer;
import sx.blah.discord.util.audio.AudioPlayer.Track;

public class AudioUpdatesTask implements Runnable {

    private final AudioPlayer player;
    private final Track track;
    private final String songId;

    public AudioUpdatesTask(AudioPlayer player, Track track, String songId) {
        this.player = player;
        this.track = track;
        this.songId = songId;
    }

    @Override
    public void run() {
        if (player.getCurrentTrack() != this.track) {
            return;
        }
        double progress = (100.0 * track.getCurrentTrackTime()) / (double) track.getTotalTrackTime();
        SongProgressMap.INSTANCE.setProgress(player.getGuild().getStringID(), SongProgress.create(songId, progress));

        AsyncService.GENERIC.schedule(this, track.getTotalTrackTime() / 200, TimeUnit.MILLISECONDS);
    }

}

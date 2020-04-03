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

import static com.google.common.base.Preconditions.checkState;

import com.google.common.eventbus.Subscribe;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrame;

import me.kenzierocks.ourtube.guildvol.GuildVolume;
import me.kenzierocks.ourtube.guildvol.SetVolume;
import sx.blah.discord.handle.audio.AudioEncodingType;
import sx.blah.discord.handle.audio.IAudioProvider;

public class AudioProvider implements IAudioProvider {

    private final AudioPlayer audioPlayer;
    private volatile int framesProvided;
    private AudioFrame lastFrame;

    public AudioProvider(String guildId, AudioPlayer audioPlayer) {
        this.audioPlayer = audioPlayer;
        GuildVolume.INSTANCE.events.subscribe(guildId, this);
        audioPlayer.setVolume((int) GuildVolume.INSTANCE.getVolume(guildId));
    }

    @Subscribe
    public void onSetVolume(SetVolume setVolume) {
        audioPlayer.setVolume((int) setVolume.getVolume());
    }

    @Override
    public boolean isReady() {
        if (lastFrame == null) {
            lastFrame = audioPlayer.provide();
        }

        return lastFrame != null;
    }

    public long getDurationProvidedMs() {
        return framesProvided * 20l;
    }

    @Override
    public byte[] provide() {
        checkState(isReady(), "Not ready!");

        byte[] data = lastFrame != null ? lastFrame.data : null;
        lastFrame = null;

        if (data != null) {
            framesProvided++;
        }

        return data;
    }

    @Override
    public int getChannels() {
        return 2;
    }

    @Override
    public AudioEncodingType getAudioEncodingType() {
        return AudioEncodingType.OPUS;
    }
}
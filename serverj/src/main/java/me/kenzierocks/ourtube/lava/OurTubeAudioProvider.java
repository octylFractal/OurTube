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

import com.google.common.eventbus.Subscribe;
import com.sedmelluq.discord.lavaplayer.format.StandardAudioDataFormats;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.playback.MutableAudioFrame;
import discord4j.core.object.util.Snowflake;
import discord4j.voice.AudioProvider;
import me.kenzierocks.ourtube.guildvol.GuildVolume;
import me.kenzierocks.ourtube.guildvol.SetVolume;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;

public class OurTubeAudioProvider extends AudioProvider {

    private final AudioPlayer audioPlayer;
    private final MutableAudioFrame frame = new MutableAudioFrame();
    private final AtomicInteger framesProvided = new AtomicInteger();

    public OurTubeAudioProvider(Snowflake guildId, AudioPlayer audioPlayer) {
        super(ByteBuffer.allocate(
            StandardAudioDataFormats.DISCORD_OPUS.maximumChunkSize()
        ));
        this.audioPlayer = audioPlayer;
        GuildVolume.INSTANCE.events.subscribe(guildId, this);
        audioPlayer.setVolume((int) GuildVolume.INSTANCE.getVolume(guildId));
        frame.setBuffer(getBuffer());
    }

    @Subscribe
    public void onSetVolume(SetVolume setVolume) {
        audioPlayer.setVolume((int) setVolume.getVolume());
    }

    public long getDurationProvidedMs() {
        return framesProvided.get() * 20L;
    }

    @Override
    public boolean provide() {
        boolean didProvide = audioPlayer.provide(frame);
        if (didProvide) {
            getBuffer().flip();
            framesProvided.getAndIncrement();
        }
        return didProvide;
    }
}

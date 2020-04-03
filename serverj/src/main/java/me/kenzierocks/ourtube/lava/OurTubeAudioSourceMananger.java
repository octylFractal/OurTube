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

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.concurrent.ExecutionException;

import com.google.common.base.Throwables;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManager;
import com.sedmelluq.discord.lavaplayer.track.AudioItem;
import com.sedmelluq.discord.lavaplayer.track.AudioReference;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;

import me.kenzierocks.ourtube.LazyInputStream;
import me.kenzierocks.ourtube.OurTube;
import me.kenzierocks.ourtube.SongData;
import me.kenzierocks.ourtube.YoutubeAccess;
import me.kenzierocks.ourtube.YoutubeStreams;
import me.kenzierocks.ourtube.lava.OurTubeAudioTrack.OurTubeMetadata;

public class OurTubeAudioSourceMananger implements AudioSourceManager {

    @Override
    public String getSourceName() {
        return "OurTube";
    }

    @Override
    public AudioItem loadItem(DefaultAudioPlayerManager manager, AudioReference reference) {
        if (!reference.identifier.startsWith("ourtube:")) {
            return null;
        }
        OurTubeItemInfo info;
        try {
            info = OurTube.MAPPER.readValue(reference.identifier.substring("ourtube:".length()), OurTubeItemInfo.class);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        SongData data;
        try {
            data = YoutubeAccess.INSTANCE.getVideoDataCached(info.getId()).get();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            Throwable t = e.getCause();
            Throwables.throwIfUnchecked(t);
            throw new RuntimeException(t);
        }
        InputStream stream = new LazyInputStream(() -> YoutubeStreams.newStream(data));
        OurTubeMetadata meta = OurTubeMetadata.createForNow(info.getSubmitter());
        return new OurTubeAudioTrack(createTrackInfo(data), meta, YoutubeStreams.annotateStream(data, stream));
    }

    private AudioTrackInfo createTrackInfo(SongData data) {
        return new AudioTrackInfo(data.getName(),
                "Unknown",
                data.getDuration(),
                data.getId(),
                true,
                data.getId());
    }

    @Override
    public boolean isTrackEncodable(AudioTrack track) {
        return false;
    }

    @Override
    public void encodeTrack(AudioTrack track, DataOutput output) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public AudioTrack decodeTrack(AudioTrackInfo trackInfo, DataInput input) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void shutdown() {
    }

}

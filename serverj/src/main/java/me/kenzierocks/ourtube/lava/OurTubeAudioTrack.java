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

import static com.google.common.base.Preconditions.checkArgument;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.time.Instant;
import java.util.Comparator;
import java.util.Optional;

import javax.sound.sampled.AudioFormat.Encoding;
import javax.sound.sampled.AudioInputStream;

import com.google.auto.value.AutoValue;
import com.google.common.collect.Comparators;
import com.sedmelluq.discord.lavaplayer.filter.AudioPipeline;
import com.sedmelluq.discord.lavaplayer.filter.AudioPipelineFactory;
import com.sedmelluq.discord.lavaplayer.filter.PcmFormat;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;
import com.sedmelluq.discord.lavaplayer.track.BaseAudioTrack;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioProcessingContext;
import com.sedmelluq.discord.lavaplayer.track.playback.LocalAudioTrackExecutor;

public class OurTubeAudioTrack extends BaseAudioTrack {

    public static final Comparator<AudioTrack> CMP_QUEUE_TIME = Comparator.comparing(
            track -> {
                return OurTubeAudioTrack.metadata(track)
                        .map(OurTubeMetadata::queueTime);
            },
            Comparators.emptiesFirst(Comparator.naturalOrder()));

    public static Optional<OurTubeAudioTrack> cast(AudioTrack track) {
        if (!(track instanceof OurTubeAudioTrack)) {
            return Optional.empty();
        }
        return Optional.of((OurTubeAudioTrack) track);
    }

    public static Optional<OurTubeMetadata> metadata(AudioTrack track) {
        return cast(track).map(t -> t.getMetadata());
    }

    @AutoValue
    public abstract static class OurTubeMetadata {

        public static OurTubeMetadata createForNow(String submitter) {
            return create(submitter, Instant.now());
        }

        public static OurTubeMetadata create(String submitter, Instant queueTime) {
            return new AutoValue_OurTubeAudioTrack_OurTubeMetadata(submitter, queueTime);
        }

        OurTubeMetadata() {
        }

        public abstract String submitter();

        public abstract Instant queueTime();

    }

    private final OurTubeMetadata metadata;
    private final AudioInputStream stream;
    private final InputStream bufferedStream;
    private final ShortBuffer inputBuffer;

    public OurTubeAudioTrack(AudioTrackInfo trackInfo, OurTubeMetadata metadata, AudioInputStream stream) {
        super(trackInfo);
        checkArgument(stream.getFormat().getEncoding() == Encoding.PCM_SIGNED, "Need PCM encoding");
        this.metadata = metadata;
        this.inputBuffer = ByteBuffer.allocateDirect(2048 * stream.getFormat().getChannels())
                .order(ByteOrder.nativeOrder())
                .asShortBuffer();
        this.stream = stream;
        bufferedStream = new BufferedInputStream(stream);
    }

    public OurTubeMetadata getMetadata() {
        return metadata;
    }

    @Override
    public void process(LocalAudioTrackExecutor executor) throws Exception {
        AudioProcessingContext ctx = executor.getProcessingContext();
        AudioPipeline downstream = AudioPipelineFactory.create(ctx,
                new PcmFormat(
                        stream.getFormat().getChannels(),
                        (int) stream.getFormat().getSampleRate()));
        try {
            executor.executeProcessingLoop(() -> {
                boolean eos = false;
                while (!eos) {
                    try {
                        eos = fillBuffer();
                    } catch (IOException e) {
                        throw new IllegalStateException(e);
                    }

                    downstream.process(inputBuffer);
                }
                downstream.flush();
            }, null);
        } finally {
            downstream.close();
        }
    }

    private boolean fillBuffer() throws IOException {
        inputBuffer.clear();
        boolean eos = false;
        while (inputBuffer.hasRemaining()) {
            int hi = bufferedStream.read();
            int lo = bufferedStream.read();
            if (lo == -1 || hi == -1) {
                eos = true;
                break;
            }
            inputBuffer.put((short) (lo | (hi << 8)));
        }
        inputBuffer.flip();
        return eos;
    }

}

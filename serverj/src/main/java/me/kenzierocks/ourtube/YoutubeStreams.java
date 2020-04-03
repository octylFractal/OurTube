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

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.BitSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;

import org.slf4j.Logger;

import com.google.common.io.ByteStreams;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

public class YoutubeStreams {

    private static final Logger LOGGER = Log.get();

    private static final AudioFormat FFMPEG_OUT_FORMAT = new AudioFormat(
            AudioFormat.Encoding.PCM_SIGNED,
            48000, // sample rate
            16, // sample size (bits)
            2, // channels
            4, // frame size (2 bytes x channels)
            48000, // frame rate
            true); // big endian

    /**
     * @return PCM audio tuned to discord4j internal specs for optimization
     */
    public static InputStream newStream(SongData songData) {
        String url = "https://www.youtube.com/watch?v=" + songData.getId();
        LOGGER.debug("{}: Acquring download...", url);
        InputStream dl = callYtdl(url);

        LOGGER.debug("{}: Transcoding...", url);
        InputStream ffmpeg = callFfmpeg(dl);
        return ffmpeg;
    }

    /**
     * @return PCM audio tuned to discord4j internal specs for optimization
     */
    public static AudioInputStream annotateStream(SongData songData, InputStream stream) {
        float frameLength = computeFrameLength(songData);
        return new AudioInputStream(stream, FFMPEG_OUT_FORMAT, (long) Math.ceil(frameLength));
    }

    private static float computeFrameLength(SongData songData) {
        // R = frame rate, in frames/sec
        // Dms = duration, in milliseconds
        // L = frame length, in frames
        // Ds = duration, in seconds
        // Ds = Dms / 1000
        // L = Ds * R
        // we move the division to R because it's lossless that way
        float frameRate = FFMPEG_OUT_FORMAT.getFrameRate();
        float frameLength = (songData.getDuration() * (frameRate / 1000f));
        return frameLength;
    }

    private static final ExecutorService CHECKER = Executors.newCachedThreadPool(
            new ThreadFactoryBuilder().setNameFormat("stream-checker-%d").setPriority(Thread.MIN_PRIORITY).build());
    private static final ExecutorService WRITER = Executors.newCachedThreadPool(
            new ThreadFactoryBuilder().setNameFormat("stream-writer-%d").build());

    private static final BitSet YTDL_OK = new BitSet();
    static {
        YTDL_OK.set(0);
        // early stream terminations -- skip song
        YTDL_OK.set(1);
    }

    private static final BitSet FFMPEG_OK = new BitSet();
    static {
        FFMPEG_OK.set(0);
        // early stream terminations -- skip song
        FFMPEG_OK.set(141);
    }

    private static InputStream callYtdl(String url) {
        try {
            Process ytdl = new ProcessBuilder("ytdl", url, "--filter", "audio")
                    .start();
            ytdl.getOutputStream().close();
            startChecker("ytdl", ytdl, YTDL_OK);
            // no buffering for this stream :)
            // it gets buffered in callFfmpeg
            return ytdl.getInputStream();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    // 128kb buffer -- audio is large :)
    private static final int LARGE_BUFFER = 128 * 1024;
    private static final long READ_TIMEOUT = TimeUnit.SECONDS.toNanos(30);

    private static InputStream callFfmpeg(InputStream source) {
        try {
            Process ffmpeg = new ProcessBuilder("ffmpeg", "-i", "pipe:0",
                    "-ar", "48000", "-ac", "2", "-acodec", "pcm_s16be", "-f", "s16be", "pipe:1")
                            .start();
            startChecker("FFmpeg", ffmpeg, FFMPEG_OK);
            WRITER.submit(() -> {
                Future<?> future = null;
                try (OutputStream out = ffmpeg.getOutputStream()) {
                    byte[] buf = new byte[LARGE_BUFFER];
                    int read;
                    while ((read = source.read(buf)) != -1) {
                        if (future != null) {
                            future.cancel(true);
                        }
                        future = CHECKER.submit(() -> {
                            TimeUnit.NANOSECONDS.sleep(READ_TIMEOUT);
                            // no reads for a while, time to exit!
                            LOGGER.warn("Closing ffmpeg input due to read timeout!");
                            source.close();
                            return null;
                        });
                        out.write(buf, 0, read);
                    }
                } finally {
                    future.cancel(true);
                    source.close();
                }
                return null;
            });
            return new BufferedInputStream(ffmpeg.getInputStream(), LARGE_BUFFER);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static void startChecker(String name, Process process, BitSet okCodes) {
        CHECKER.submit(() -> {
            // copy in error stream so we can dump it on error
            InputStream buffered = new BufferedInputStream(process.getErrorStream());
            ByteArrayOutputStream cap = new ByteArrayOutputStream();
            if (Environment.INTERNAL_STREAMS_ERROR_OUTPUT) {
                byte[] buf = new byte[8192];
                int read;
                while ((read = buffered.read(buf)) != -1) {
                    System.err.write(buf, 0, read);
                    cap.write(buf, 0, read);
                }
            } else {
                ByteStreams.copy(buffered, cap);
            }
            String errors = StandardCharsets.UTF_8.decode(ByteBuffer.wrap(cap.toByteArray())).toString();
            if (!okCodes.get(process.waitFor())) {
                LOGGER.error("Error with " + name + " (exit code " + process.exitValue() + "): "
                        + errors);
            } else {
                LOGGER.debug("{} exited cleanly.", name);
            }
            return null;
        });
    }

}

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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.security.GeneralSecurityException;
import java.time.Duration;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.Video;
import com.google.api.services.youtube.model.VideoContentDetails;
import com.google.api.services.youtube.model.VideoListResponse;
import com.google.api.services.youtube.model.VideoSnippet;
import com.google.common.util.concurrent.ListenableFuture;

import me.kenzierocks.ourtube.SongData.Thumbnail;

public enum YoutubeAccess {
    INSTANCE;

    private static YouTube initYoutube() {
        try {
            HttpTransport transport = GoogleNetHttpTransport.newTrustedTransport();
            JsonFactory json = JacksonFactory.getDefaultInstance();
            return new YouTube.Builder(transport, json, request -> {
            }).setApplicationName("OurTube").build();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    private final YouTube yt3 = initYoutube();

    public ListenableFuture<SongData> getVideoData(String songId) {
        return AsyncService.GENERIC.submit(() -> {
            VideoListResponse ytResponse = yt3.videos().list("snippet,contentDetails")
                    .setId(songId)
                    .setKey(Environment.YOUTUBE_API_KEY)
                    .execute();
            if (ytResponse.isEmpty()) {
                throw new IllegalArgumentException("No items, invalid ID?");
            }
            Video video = ytResponse.getItems().get(0);
            VideoContentDetails content = video.getContentDetails();
            VideoSnippet snip = video.getSnippet();
            com.google.api.services.youtube.model.Thumbnail ytThumb = snip.getThumbnails().getMedium();
            return SongData.builder()
                    .id(video.getId())
                    .name(snip.getTitle())
                    .thumbnail(Thumbnail.of(ytThumb.getHeight(), ytThumb.getWidth(), ytThumb.getUrl()))
                    .duration(Duration.parse(content.getDuration()).toMillis())
                    .build();
        });
    }

}

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

import static com.google.common.collect.ImmutableList.toImmutableList;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.GeneralSecurityException;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.slf4j.Logger;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.repackaged.com.google.common.base.Splitter;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.YouTube.PlaylistItems;
import com.google.api.services.youtube.model.PlaylistItem;
import com.google.api.services.youtube.model.PlaylistItemContentDetails;
import com.google.api.services.youtube.model.PlaylistItemListResponse;
import com.google.api.services.youtube.model.Video;
import com.google.api.services.youtube.model.VideoContentDetails;
import com.google.api.services.youtube.model.VideoListResponse;
import com.google.api.services.youtube.model.VideoSnippet;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.util.concurrent.ListenableFuture;

import io.netty.handler.codec.http.QueryStringDecoder;
import me.kenzierocks.ourtube.SongData.Thumbnail;

public enum YoutubeAccess {
    INSTANCE;

    private static final Logger LOGGER = Log.get();

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

    private final LoadingCache<String, ListenableFuture<SongData>> videoDataCache =
            CacheBuilder.newBuilder()
                    .maximumSize(1000)
                    .expireAfterAccess(1, TimeUnit.HOURS)
                    .build(CacheLoader.from(this::getVideoData));

    public ListenableFuture<SongData> getVideoDataCached(String songId) {
        return videoDataCache.getUnchecked(songId);
    }

    private ListenableFuture<SongData> getVideoData(String songId) {
        return AsyncService.GENERIC.submit(() -> {
            VideoListResponse ytResponse = yt3.videos().list("snippet,contentDetails")
                    .setId(songId)
                    .setKey(Environment.YOUTUBE_API_KEY)
                    .execute();
            if (ytResponse.getItems().isEmpty()) {
                LOGGER.warn("Missing video info for item '{}'", songId);
                return SongData.builder()
                        .id(songId)
                        .name("Warning: Unknown - " + songId)
                        .thumbnail(Thumbnail.of(320, 320, "https://i.imgur.com/3FIBbnxm.png"))
                        .duration(Integer.MAX_VALUE)
                        .build();
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

    public List<String> getSongIds(String songUrl) {
        List<String> ids = getSongIdsInternal(songUrl);
        if (ids.isEmpty()) {
            LOGGER.warn("No IDs discovered for '{}'!", songUrl);
        }
        return ids;
    }

    private List<String> getSongIdsInternal(String songUrl) {
        URI url;
        try {
            url = new URI(songUrl);
        } catch (URISyntaxException e) {
            LOGGER.debug("Bad song URL provided", e);
            return ImmutableList.of();
        }
        if (url.getHost().endsWith("youtube.com")) {
            QueryStringDecoder decoder = new QueryStringDecoder(url);
            switch (url.getPath()) {
                case "/watch":
                    List<String> watch = decoder.parameters().get("v");
                    LOGGER.debug("/watch IDs: {}", watch);
                    return watch == null ? ImmutableList.of() : ImmutableList.copyOf(watch);
                case "/playlist":
                    String list = Iterables.getFirst(decoder.parameters().get("list"), null);
                    if (list == null) {
                        return ImmutableList.of();
                    }
                    List<String> playlistSongIds = getPlaylistSongIds(list);
                    LOGGER.debug("/playlist IDs: {}", playlistSongIds);
                    return playlistSongIds;
                default:
                    return ImmutableList.of();
            }
        }
        if (url.getHost().equals("youtu.be")) {
            Iterable<String> pathParts = Splitter.on('/').omitEmptyStrings().split(url.getPath());
            ImmutableList<String> beIds = ImmutableList.copyOf(Iterables.limit(pathParts, 1));
            LOGGER.debug("youtu.be IDs: {}", beIds);
            return beIds;
        }
        return ImmutableList.of();
    }

    private List<String> getPlaylistSongIds(String list) {
        try {
            return playlistStream(list)
                    .map(PlaylistItem::getContentDetails)
                    .filter(Objects::nonNull)
                    .map(PlaylistItemContentDetails::getVideoId)
                    .collect(toImmutableList());
        } catch (Exception e) {
            LOGGER.error("Error retrieving playlist items", e);
            return ImmutableList.of();
        }

    }

    private Stream<PlaylistItem> playlistStream(String list) throws IOException {
        Stream<PlaylistItem> current = Stream.of();
        String pageToken = "";
        while (pageToken != null) {
            PlaylistItems.List itemRequest = yt3.playlistItems().list("contentDetails")
                    .setPlaylistId(list)
                    .setKey(Environment.YOUTUBE_API_KEY)
                    .setMaxResults(50L);
            if (!pageToken.isEmpty()) {
                itemRequest.setPageToken(pageToken);
            }
            PlaylistItemListResponse response = itemRequest
                    .execute();
            current = Stream.concat(current, response.getItems().stream());
            pageToken = response.getNextPageToken();
        }
        return current;
    }

}

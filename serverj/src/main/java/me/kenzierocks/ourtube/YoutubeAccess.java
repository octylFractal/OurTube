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

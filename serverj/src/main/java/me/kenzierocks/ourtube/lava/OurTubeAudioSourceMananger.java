package me.kenzierocks.ourtube.lava;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutionException;

import com.google.common.base.Throwables;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManager;
import com.sedmelluq.discord.lavaplayer.track.AudioItem;
import com.sedmelluq.discord.lavaplayer.track.AudioReference;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo;

import me.kenzierocks.ourtube.LazyInputStream;
import me.kenzierocks.ourtube.SongData;
import me.kenzierocks.ourtube.YoutubeAccess;
import me.kenzierocks.ourtube.YoutubeStreams;

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
        String id = reference.identifier.substring("ourtube:".length());
        SongData data;
        try {
            data = YoutubeAccess.INSTANCE.getVideoData(id).get();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            Throwable t = e.getCause();
            Throwables.throwIfUnchecked(t);
            throw new RuntimeException(t);
        }
        InputStream stream = new LazyInputStream(() -> YoutubeStreams.newStream(data));
        return new OurTubeAudioTrack(createTrackInfo(data), YoutubeStreams.annotateStream(data, stream));
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

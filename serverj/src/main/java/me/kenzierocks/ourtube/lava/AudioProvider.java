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
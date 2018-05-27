package me.kenzierocks.ourtube.lava;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;

import me.kenzierocks.ourtube.AsyncService;
import me.kenzierocks.ourtube.AudioUpdatesTask;
import me.kenzierocks.ourtube.Dissy;
import me.kenzierocks.ourtube.Log;
import me.kenzierocks.ourtube.guildqueue.GuildQueue;
import me.kenzierocks.ourtube.guildqueue.PopSong;
import me.kenzierocks.ourtube.guildqueue.PushSong;

public class TrackScheduler extends AudioEventAdapter {

    private static final Logger LOGGER = Log.get();

    private final String guildId;
    private final AudioPlayer player;
    private final BlockingQueue<AudioTrack> queue;
    private final Lock accessLock = new ReentrantLock();

    public TrackScheduler(String guildId, AudioPlayer player) {
        this.guildId = guildId;
        this.player = player;
        player.addListener(this);
        this.queue = new LinkedBlockingQueue<>();
    }

    public BlockingQueue<AudioTrack> getQueue() {
        return queue;
    }

    public Lock getAccessLock() {
        return accessLock;
    }

    public void addTrack(AudioTrack track) {
        accessLock.lock();
        try {
            LOGGER.debug("Queued track " + track.getIdentifier());
            queue.add(track);
            GuildQueue.getSongId(track)
                    .ifPresent(songId -> {
                        GuildQueue.INSTANCE.events.post(guildId, PushSong.create(songId));
                    });
            player.startTrack(track, true);
        } finally {
            accessLock.unlock();
        }
    }

    public void nextTrack() {
        accessLock.lock();
        try {
            AudioTrack nextTrack = queue.peek();
            if (nextTrack == null) {
                return;
            }
            LOGGER.debug("Started track " + nextTrack.getIdentifier());
            player.startTrack(nextTrack, false);
        } finally {
            accessLock.unlock();
        }
    }

    public void skipTrack() {
        queue.poll();
        nextTrack();
    }

    @Override
    public void onTrackStart(AudioPlayer player, AudioTrack track) {
        GuildQueue.getSongId(track)
                .ifPresent(songId -> {
                    AsyncService.GENERIC.execute(new AudioUpdatesTask(
                            player,
                            Dissy.getProvider(guildId),
                            track,
                            guildId,
                            songId));
                });
    }

    @Override
    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
        accessLock.lock();
        try {
            GuildQueue.getSongId(track)
                    .ifPresent(songId -> {
                        GuildQueue.INSTANCE.events.post(guildId, PopSong.create(songId));
                    });
            if (endReason.mayStartNext) {
                queue.poll();
                nextTrack();
            }
        } finally {
            accessLock.unlock();
        }
    }
}
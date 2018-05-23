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
package me.kenzierocks.ourtube.guildqueue;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;

import javax.sound.sampled.AudioInputStream;

import org.slf4j.Logger;

import com.google.common.collect.Lists;
import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import me.kenzierocks.ourtube.AsyncService;
import me.kenzierocks.ourtube.AudioUpdatesTask;
import me.kenzierocks.ourtube.Dissy;
import me.kenzierocks.ourtube.Events;
import me.kenzierocks.ourtube.LazyInputStream;
import me.kenzierocks.ourtube.Log;
import me.kenzierocks.ourtube.SongData;
import me.kenzierocks.ourtube.YoutubeAccess;
import me.kenzierocks.ourtube.YoutubeStreams;
import me.kenzierocks.ourtube.guildvol.GuildVolume;
import me.kenzierocks.ourtube.guildvol.SetVolume;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.util.audio.AudioPlayer;
import sx.blah.discord.util.audio.AudioPlayer.Track;
import sx.blah.discord.util.audio.events.AudioPlayerEvent;
import sx.blah.discord.util.audio.events.AudioPlayerInitEvent;
import sx.blah.discord.util.audio.events.TrackFinishEvent;
import sx.blah.discord.util.audio.events.TrackQueueEvent;
import sx.blah.discord.util.audio.events.TrackStartEvent;

public enum GuildQueue {
    INSTANCE;

    private static final Logger LOGGER = Log.get();

    public static String getSongId(Track track) {
        return checkNotNull(track.getMetadata().get("songId"), "missing songId").toString();
    }

    public final Events events = new Events("GuildQueue");

    public static AudioPlayer getPlayer(String guildId) {
        return AudioPlayer.getAudioPlayerForGuild(Dissy.BOT.getGuildByID(Long.parseUnsignedLong(guildId)));
    }

    public void queueSongs(String guildId, String songUrl) {
        AudioPlayer player = getPlayer(guildId);
        List<String> songIds = YoutubeAccess.INSTANCE.getSongIds(songUrl);
        ListenableFuture<List<Track>> allSongs = Futures.allAsList(Lists.transform(songIds, this::createTrack));
        Futures.addCallback(allSongs, new FutureCallback<List<Track>>() {

            @Override
            public void onSuccess(List<Track> result) {
                result.forEach(player::queue);
            }

            @Override
            public void onFailure(Throwable t) {
                LOGGER.error("Error queueing songs from " + songUrl, t);
            }

        }, AsyncService.GENERIC);
    }

    private FluentFuture<Track> createTrack(String id) {
        FluentFuture<SongData> songData = FluentFuture.from(YoutubeAccess.INSTANCE.getVideoData(id));
        FluentFuture<AudioInputStream> pcmData = songData.transform(
                data -> {
                    return YoutubeStreams.annotateStream(data, new LazyInputStream(
                            () -> YoutubeStreams.newStream(data)));
                },
                AsyncService.GENERIC);
        FluentFuture<Track> track = pcmData.transform(pcm -> {
            Track t = new Track(pcm);
            t.getMetadata().put("songId", id);
            return t;
        }, AsyncService.GENERIC);
        return track;
    }

    public void skipSong(String guildId, String songId) {
        AudioPlayer player = getPlayer(guildId);
        Track latest = player.getCurrentTrack();
        if (latest == null || !getSongId(latest).equals(songId)) {
            return;
        }
        Track skipped = player.skip();
        if (skipped == null) {
            return;
        }

        String songIdSkipped = getSongId(skipped);
        events.post(guildId, PopSong.create(songIdSkipped));
    }

    private final class GQListener {

        private final String guildId;
        private final long guildIdLong;

        public GQListener(String guildId) {
            this.guildId = guildId;
            this.guildIdLong = Long.parseUnsignedLong(guildId);
            GuildVolume.INSTANCE.events.subscribe(guildId, this);
            getPlayer(guildId).setVolume(GuildVolume.INSTANCE.getVolume(guildId) / 100);
        }

        @Subscribe
        public void onSetVolume(SetVolume setVolume) {
            getPlayer(guildId).setVolume(setVolume.getVolume() / 100);
        }

        private boolean notOurEvent(AudioPlayerEvent event) {
            return event.getPlayer().getGuild().getLongID() != guildIdLong;
        }

        private boolean notOurTrack(Track track) {
            return !track.getMetadata().containsKey("songId");
        }

        @EventSubscriber
        public void onTrackQueue(TrackQueueEvent event) {
            if (notOurEvent(event) || notOurTrack(event.getTrack())) {
                return;
            }
            String songId = getSongId(event.getTrack());
            events.post(guildId, PushSong.create(songId));
        }

        @EventSubscriber
        public void onTrackStart(TrackStartEvent event) {
            if (notOurEvent(event) || notOurTrack(event.getTrack())) {
                return;
            }
            String songId = getSongId(event.getTrack());
            AsyncService.GENERIC.execute(new AudioUpdatesTask(getPlayer(guildId), event.getTrack(), songId));
        }

        @EventSubscriber
        public void onTrackFinish(TrackFinishEvent event) {
            if (notOurEvent(event) || notOurTrack(event.getOldTrack())) {
                return;
            }
            String songId = getSongId(event.getOldTrack());
            events.post(guildId, PopSong.create(songId));
        }

    }

    @EventSubscriber
    public void onNewPlayer(AudioPlayerInitEvent event) {
        String guildId = event.getPlayer().getGuild().getStringID();
        event.getClient().getDispatcher().registerListener(new GQListener(guildId));
    }

}

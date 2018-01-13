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

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.sound.sampled.AudioInputStream;

import org.slf4j.Logger;

import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.FluentFuture;
import com.google.common.util.concurrent.FutureCallback;

import me.kenzierocks.ourtube.events.SkipSong;
import me.kenzierocks.ourtube.guildchannels.GuildChannels;
import me.kenzierocks.ourtube.guildchannels.NewChannel;
import me.kenzierocks.ourtube.guildqueue.GuildQueue;
import me.kenzierocks.ourtube.guildqueue.PushSong;
import sx.blah.discord.api.ClientBuilder;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.api.events.IListener;
import sx.blah.discord.handle.impl.events.guild.GuildCreateEvent;
import sx.blah.discord.handle.impl.events.guild.voice.user.UserVoiceChannelJoinEvent;
import sx.blah.discord.handle.impl.events.guild.voice.user.UserVoiceChannelLeaveEvent;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IVoiceChannel;
import sx.blah.discord.util.audio.AudioPlayer;
import sx.blah.discord.util.audio.AudioPlayer.Track;
import sx.blah.discord.util.audio.events.AudioPlayerEvent;
import sx.blah.discord.util.audio.events.TrackFinishEvent;
import sx.blah.discord.util.audio.events.TrackSkipEvent;
import sx.blah.discord.util.audio.events.TrackStartEvent;

public class Dissy {

    public static final IDiscordClient BOT = new ClientBuilder()
            .withToken(Environment.DISCORD_TOKEN)
            .registerListener(guildSubscriber())
            .login();

    public static final Events events = new Events("dissy");

    private static final class OurTubePipe {

        private static final Logger LOGGER = Log.get();

        private final String guildId;
        private final AudioPlayer player;
        private final Lock waitLock = new ReentrantLock();
        private boolean waiting;

        public OurTubePipe(String guildId, AudioPlayer player) {
            this.guildId = guildId;
            this.player = player;
        }

        @EventSubscriber
        public void onTrackStart(TrackStartEvent event) {
            if (event.getPlayer().getGuild().getLongID() != player.getGuild().getLongID()) {
                return;
            }
            Track track = event.getTrack();
            String songId = (String) track.getMetadata().get("songId");
            LOGGER.info("{}: Started playing", songId);
            AsyncService.GENERIC.submit(new AudioUpdatesTask(player, track, songId));
        }

        @EventSubscriber
        public void onTrackFinish(TrackFinishEvent event) {
            onTrackEnd(event);
        }

        @EventSubscriber
        public void onTrackSkip(TrackSkipEvent event) {
            onTrackEnd(event);
        }

        private void onTrackEnd(AudioPlayerEvent event) {
            if (event.getPlayer().getGuild().getLongID() != player.getGuild().getLongID()) {
                return;
            }
            BOT.getDispatcher().unregisterListener(this);
            GuildQueue.INSTANCE.popSong(guildId);
            // don't block the threads, idiot!
            AsyncService.GENERIC.execute(() -> playNext());
        }

        @Subscribe
        public void onPushSong(PushSong event) {
            waitLock.lock();
            try {
                GuildQueue.INSTANCE.events.unsubscribe(guildId, this);
                playNext();
                waiting = false;
            } finally {
                waitLock.unlock();
            }
        }

        @Subscribe
        public void onSkipSong(SkipSong event) {
            waitLock.lock();
            try {
                if (!waiting) {
                    player.skip();
                }
            } finally {
                waitLock.unlock();
            }
        }

        /**
         * Called to play the next queued song, or await it.
         */
        public void playNext() {
            waitLock.lock();
            try {
                GuildQueue.INSTANCE.useQueue(guildId, queue -> {
                    if (queue.isEmpty()) {
                        waiting = true;
                        GuildQueue.INSTANCE.events.subscribe(guildId, this);
                    } else {
                        play(queue.peekFirst());
                    }
                });
            } finally {
                waitLock.unlock();
            }
        }

        private void play(String songId) {
            FluentFuture<SongData> songData = FluentFuture.from(YoutubeAccess.INSTANCE.getVideoData(songId));
            FluentFuture<AudioInputStream> pcmData = songData.transform(YoutubeStreams::newStream, AsyncService.GENERIC);
            pcmData.addCallback(new FutureCallback<AudioInputStream>() {

                @Override
                public void onSuccess(AudioInputStream pcmData) {
                    LOGGER.info("{}: queued", songId);
                    BOT.getDispatcher().registerListener(this);
                    Track track = new Track(pcmData);
                    track.getMetadata().put("songId", songId);
                    player.queue(track);
                }

                @Override
                public void onFailure(Throwable t) {
                    LOGGER.error("Error queueing track " + songId, t);
                }
            }, AsyncService.GENERIC);
        }

    }

    private static final class Subscriber {

        private final IGuild guild;

        public Subscriber(IGuild guild) {
            this.guild = guild;
        }

        @Subscribe
        public void onNewChannel(NewChannel event) {
            IVoiceChannel channel = guild.getVoiceChannelByID(Long.parseUnsignedLong(event.getChannelId()));
            if (channel == null) {
                return;
            }
            IListener<UserVoiceChannelJoinEvent> listener = new IListener<UserVoiceChannelJoinEvent>() {

                @Override
                public void handle(UserVoiceChannelJoinEvent joinEvent) {
                    if (joinEvent.getVoiceChannel().getLongID() != channel.getLongID()) {
                        return;
                    }
                    if (joinEvent.getUser().getLongID() != BOT.getOurUser().getLongID()) {
                        return;
                    }
                    BOT.getDispatcher().unregisterListener(this);
                    AudioPlayer player = AudioPlayer.getAudioPlayerForGuild(guild);

                    OurTubePipe pipe = new OurTubePipe(guild.getStringID(), player);
                    BOT.getDispatcher().registerListener(new Object() {

                        @EventSubscriber
                        public void onLeaveChannel(UserVoiceChannelLeaveEvent leaveEvent) {
                            if (leaveEvent.getVoiceChannel().getLongID() != channel.getLongID()) {
                                return;
                            }
                            if (leaveEvent.getUser().getLongID() != BOT.getOurUser().getLongID()) {
                                return;
                            }
                            player.clear();
                            events.unsubscribe(guild.getStringID(), pipe);
                        }
                    });
                    events.subscribe(guild.getStringID(), pipe);
                    pipe.playNext();
                }
            };
            BOT.getDispatcher().registerListener(listener);
            channel.join();
        }

    }

    private static IListener<GuildCreateEvent> guildSubscriber() {
        return event -> {
            String guildId = event.getGuild().getStringID();
            GuildChannels.INSTANCE.events.subscribe(guildId, new Subscriber(event.getGuild()));
        };
    }

    private Dissy() {
    }

}

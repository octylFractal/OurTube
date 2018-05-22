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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIONamespace;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.eventbus.Subscribe;

import me.kenzierocks.ourtube.guildchannels.GuildChannels;
import me.kenzierocks.ourtube.guildchannels.NewChannel;
import me.kenzierocks.ourtube.guildqueue.GuildQueue;
import me.kenzierocks.ourtube.guildqueue.PopSong;
import me.kenzierocks.ourtube.guildqueue.PushSong;
import me.kenzierocks.ourtube.guildvol.GuildVolume;
import me.kenzierocks.ourtube.guildvol.SetVolume;
import me.kenzierocks.ourtube.response.Response;
import me.kenzierocks.ourtube.songprogress.NewProgress;
import me.kenzierocks.ourtube.songprogress.SongProgress;
import me.kenzierocks.ourtube.songprogress.SongProgressMap;
import sx.blah.discord.handle.obj.IGuild;

public class OurTubeApi {

    private final SocketIONamespace server;

    public OurTubeApi(SocketIONamespace socketIONamespace) {
        this.server = socketIONamespace;
    }

    public void configure() {
        setupSongQueue();
        setupYoutubeQueries();
        setupDiscordQueries();
        setupEvents();
    }

    private final class Subscription {

        private final SocketIOClient client;
        private final String guildId;

        Subscription(SocketIOClient client, String guildId) {
            this.client = client;
            this.guildId = guildId;
        }

        @Subscribe
        public void onPush(PushSong push) {
            client.sendEvent("songQueue.queued", push);
        }

        @Subscribe
        public void onPop(PopSong pop) {
            client.sendEvent("songQueue.popped", pop);
        }

        @Subscribe
        public void onNewProgress(NewProgress progress) {
            client.sendEvent("songQueue.progress", progress.getProgress());
        }

        @Subscribe
        public void onSetVolume(SetVolume volume) {
            client.sendEvent("songQueue.volume", volume);
        }

    }

    private void songQueueUnsubscribe(Subscription subscription) {
        GuildQueue.INSTANCE.events.unsubscribe(subscription.guildId, subscription);
        SongProgressMap.INSTANCE.events.unsubscribe(subscription.guildId, subscription);
        GuildVolume.INSTANCE.events.unsubscribe(subscription.guildId, subscription);
    }

    private void setupSongQueue() {
        Map<UUID, Subscription> subscriptions = new HashMap<>();
        server.addEventListener("songQueue.subscribe", String.class, (client, guildId, ack) -> {
            UUID sessId = client.getSessionId();
            if (subscriptions.containsKey(sessId)) {
                songQueueUnsubscribe(subscriptions.get(sessId));
            }

            Subscription subs = new Subscription(client, guildId);
            GuildQueue.INSTANCE.events.subscribe(guildId, subs);
            SongProgressMap.INSTANCE.events.subscribe(guildId, subs);
            subscriptions.put(sessId, subs);

            // emit the entire queue for this guildId to the client
            GuildQueue.getPlayer(guildId).getPlaylist().stream()
                    .map(GuildQueue::getSongId)
                    .map(PushSong::create)
                    .forEach(subs::onPush);

            SongProgress progress = SongProgressMap.INSTANCE.getProgress(guildId);
            if (progress != null) {
                subs.onNewProgress(NewProgress.create(progress));
            }
            subs.onSetVolume(SetVolume.create(GuildVolume.INSTANCE.getVolume(guildId)));
            GuildVolume.INSTANCE.events.subscribe(guildId, subs);
        });
        server.addEventListener("songQueue.unsubscribe", Void.class, (client, nothing, ack) -> {
            UUID sessId = client.getSessionId();
            if (subscriptions.containsKey(sessId)) {
                songQueueUnsubscribe(subscriptions.remove(sessId));
            }
        });
        server.addMultiTypeEventListener("songQueue.queue", (client, args, ack) -> {
            String guildId = args.get(0);
            String songUrl = args.get(1);
            GuildQueue.INSTANCE.queueSongs(guildId, songUrl);
        }, String.class, String.class);
        server.addMultiTypeEventListener("songQueue.setVolume", (client, args, ack) -> {
            String guildId = args.get(0);
            Float volume = args.get(1);
            if (volume == null) {
                return;
            }
            GuildVolume.INSTANCE.setVolume(guildId, volume);
        }, String.class, Float.class);

        server.addDisconnectListener(disconClient -> {
            Subscription s = subscriptions.remove(disconClient.getSessionId());
            if (s != null) {
                songQueueUnsubscribe(s);
            }
        });
    }

    private void setupYoutubeQueries() {
        server.addEventListener("yt.songData", String.class, (client, songId, ack) -> {
            AsyncService.ackFuture("yt.songData", ack,
                    Response.from(YoutubeAccess.INSTANCE.getVideoData(songId)));
        });
    }

    private final class DissyChannel {

        @JsonProperty
        public final String id;
        @JsonProperty
        public final String name;

        public DissyChannel(String id, String name) {
            this.id = id;
            this.name = name;
        }

    }

    private final class DissySub {

        private final SocketIOClient client;
        private final String guildId;

        DissySub(SocketIOClient client, String guildId) {
            this.client = client;
            this.guildId = guildId;
        }

        @Subscribe
        public void onNewChannel(NewChannel event) {
            client.sendEvent("dis.selectedChannel", event);
        }
    }

    private void disUnsubscribe(DissySub subscription) {
        GuildChannels.INSTANCE.events.unsubscribe(subscription.guildId, subscription);
    }

    private void setupDiscordQueries() {
        server.addEventListener("dis.filterGuilds", String[].class, (client, guildIds, ack) -> {
            AsyncService.ackCallable("dis.filterGuilds", ack, () -> {
                return Stream.of(guildIds)
                        .filter(gid -> Dissy.BOT.getGuildByID(Long.parseUnsignedLong(gid)) != null)
                        .collect(toImmutableList());
            });
        });
        server.addEventListener("dis.channels", String.class, (client, guildId, ack) -> {
            AsyncService.ackCallable("dis.channels", ack, () -> {
                IGuild guild = Dissy.BOT.getGuildByID(Long.parseUnsignedLong(guildId));
                if (guild == null) {
                    throw new IllegalArgumentException("Invalid guild ID.");
                }
                return guild.getVoiceChannels().stream()
                        .map(ic -> new DissyChannel(ic.getStringID(), ic.getName()))
                        .collect(toImmutableList());
            });
        });
        server.addMultiTypeEventListener("dis.selectChannel", (client, args, ack) -> {
            GuildChannels.INSTANCE.setChannel(args.get(0), args.get(1));
        }, String.class, String.class);

        Map<UUID, DissySub> subscriptions = new HashMap<>();
        server.addEventListener("dis.subscribe", String.class, (client, guildId, ack) -> {
            UUID sessId = client.getSessionId();
            if (subscriptions.containsKey(sessId)) {
                disUnsubscribe(subscriptions.get(sessId));
            }

            DissySub sub = new DissySub(client, guildId);

            String channel = GuildChannels.INSTANCE.getChannel(guildId);
            sub.onNewChannel(NewChannel.create(channel));

            GuildChannels.INSTANCE.events.subscribe(guildId, sub);

            subscriptions.put(client.getSessionId(), sub);
        });

        server.addEventListener("dis.unsubscribe", Void.class, (client, nothing, ack) -> {
            UUID sessId = client.getSessionId();
            if (subscriptions.containsKey(sessId)) {
                disUnsubscribe(subscriptions.remove(sessId));
            }
        });
        server.addDisconnectListener(disconClient -> {
            DissySub s = subscriptions.remove(disconClient.getSessionId());
            if (s != null) {
                disUnsubscribe(s);
            }
        });
    }

    private void setupEvents() {
        server.addMultiTypeEventListener("event.skipSong", (client, args, ack) -> {
            String guildId = args.get(0);
            String songId = args.get(1);
            if (songId == null) {
                return;
            }
            GuildQueue.INSTANCE.skipSong(guildId, songId);
        }, String.class, String.class);
    }

}

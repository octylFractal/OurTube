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

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.eventbus.Subscribe;

import me.kenzierocks.ourtube.guildchannels.GuildChannels;
import me.kenzierocks.ourtube.guildchannels.NewChannel;
import me.kenzierocks.ourtube.guildqueue.GuildQueue;
import me.kenzierocks.ourtube.guildqueue.PopSong;
import me.kenzierocks.ourtube.guildqueue.PushSong;
import me.kenzierocks.ourtube.guildvol.GuildVolume;
import me.kenzierocks.ourtube.guildvol.SetVolume;
import me.kenzierocks.ourtube.lava.OurTubeAudioTrack;
import me.kenzierocks.ourtube.rpc.RpcClient;
import me.kenzierocks.ourtube.rpc.RpcDisconnect;
import me.kenzierocks.ourtube.rpc.RpcEventHandler;
import me.kenzierocks.ourtube.rpc.RpcRegistry;
import me.kenzierocks.ourtube.songprogress.NewProgress;
import me.kenzierocks.ourtube.songprogress.SongProgress;
import me.kenzierocks.ourtube.songprogress.SongProgressMap;
import sx.blah.discord.handle.obj.IGuild;

public class OurTubeApi {

    private final RpcRegistry server;

    public OurTubeApi(RpcRegistry socketIONamespace) {
        this.server = socketIONamespace;
    }

    public void configure() {
        setupSongQueue();
        setupYoutubeQueries();
        setupDiscordQueries();
        setupEvents();
    }

    private final class Subscription {

        private final RpcClient client;
        private final String guildId;

        Subscription(RpcClient client, String guildId) {
            this.client = client;
            this.guildId = guildId;
        }

        @Subscribe
        public void onPush(PushSong push) {
            client.callFunction("songQueue.queued", push);
        }

        @Subscribe
        public void onPop(PopSong pop) {
            client.callFunction("songQueue.popped", pop);
        }

        @Subscribe
        public void onNewProgress(NewProgress progress) {
            client.callFunction("songQueue.progress", progress.getProgress());
        }

        @Subscribe
        public void onSetVolume(SetVolume volume) {
            client.callFunction("songQueue.volume", volume);
        }

    }

    private void songQueueUnsubscribe(Subscription subscription) {
        GuildQueue.INSTANCE.events.unsubscribe(subscription.guildId, subscription);
        SongProgressMap.INSTANCE.events.unsubscribe(subscription.guildId, subscription);
        GuildVolume.INSTANCE.events.unsubscribe(subscription.guildId, subscription);
    }

    private static final class QueueSongs {

        public String guildId;
        public String songUrl;
    }

    private static final class ApiSetVolume {

        public String guildId;
        public float volume;
    }

    private void setupSongQueue() {
        Map<String, Subscription> subscriptions = new ConcurrentHashMap<>();
        server.register("songQueue.subscribe", RpcEventHandler.typed(String.class, (client, guildId) -> {
            String sessId = client.getId();
            if (subscriptions.containsKey(sessId)) {
                songQueueUnsubscribe(subscriptions.get(sessId));
            }

            Subscription subs = new Subscription(client, guildId);
            GuildQueue.INSTANCE.events.subscribe(guildId, subs);
            SongProgressMap.INSTANCE.events.subscribe(guildId, subs);
            subscriptions.put(sessId, subs);

            // emit the entire queue for this guildId to the client
            Stream.concat(
                    Stream.of(Dissy.getPlayer(guildId).getPlayingTrack()),
                    Dissy.getScheduler(guildId)
                            .allTracksStream())
                    .map(tr -> OurTubeAudioTrack.cast(tr))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .map(ot -> {
                        String submitter = ot.getMetadata().submitter();
                        String nick = Dissy.getNicknameForUserInGuild(guildId, submitter);
                        return PushSong.create(ot.getIdentifier(), nick);
                    })
                    .forEach(subs::onPush);

            SongProgress progress = SongProgressMap.INSTANCE.getProgress(guildId);
            if (progress != null) {
                subs.onNewProgress(NewProgress.create(progress));
            }
            subs.onSetVolume(SetVolume.create(GuildVolume.INSTANCE.getVolume(guildId)));
            GuildVolume.INSTANCE.events.subscribe(guildId, subs);
        }));
        server.register("songQueue.unsubscribe", RpcEventHandler.typed(Void.class, (client, nothing) -> {
            String sessId = client.getId();
            if (subscriptions.containsKey(sessId)) {
                songQueueUnsubscribe(subscriptions.remove(sessId));
            }
        }));
        server.register("songQueue.queue", RpcEventHandler.typed(QueueSongs.class, (client, queueSongs) -> {
            GuildQueue.INSTANCE.queueSongs(queueSongs.guildId, client.getUserId(), queueSongs.songUrl);
        }));
        server.register("songQueue.setVolume", RpcEventHandler.typed(ApiSetVolume.class, (client, setVolume) -> {
            GuildVolume.INSTANCE.setVolume(setVolume.guildId, client.getUserId(), setVolume.volume);
        }));

        server.getEvents().register(new Object() {

            @Subscribe
            public void onDisconnect(RpcDisconnect disconnect) {
                Subscription s = subscriptions.remove(disconnect.getClient().getId());
                if (s != null) {
                    songQueueUnsubscribe(s);
                }
            }
        });
    }

    private static final class GetSongData {

        public String songId;
        public String callbackName;
    }

    private void setupYoutubeQueries() {
        server.register("yt.songData", RpcEventHandler.typed(GetSongData.class, (client, data) -> {
            AsyncService.asyncResponse(client, data.callbackName, YoutubeAccess.INSTANCE.getVideoDataCached(data.songId));
        }));
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

        private final RpcClient client;
        private final String guildId;

        DissySub(RpcClient client, String guildId) {
            this.client = client;
            this.guildId = guildId;
        }

        @Subscribe
        public void onNewChannel(NewChannel event) {
            client.callFunction("dis.selectedChannel", event);
        }
    }

    private void disUnsubscribe(DissySub subscription) {
        GuildChannels.INSTANCE.events.unsubscribe(subscription.guildId, subscription);
    }

    private static final class FilterGuilds {

        public String[] guildIds;
        public String callbackName;
    }

    private static final class GetVoiceChannels {

        public String guildId;
        public String callbackName;
    }

    private static final class SelectChannel {

        public String guildId;
        public String channelId;
    }

    private void setupDiscordQueries() {
        server.register("dis.filterGuilds", RpcEventHandler.typed(FilterGuilds.class, (client, args) -> {
            AsyncService.asyncResponse(client, args.callbackName,
                    () -> Stream.of(args.guildIds)
                            .filter(gid -> Dissy.BOT.getGuildByID(Long.parseUnsignedLong(gid)) != null)
                            .collect(toImmutableList()));
        }));
        server.register("dis.channels", RpcEventHandler.typed(GetVoiceChannels.class, (client, args) -> {
            AsyncService.asyncResponse(client, args.callbackName,
                    () -> {
                        IGuild guild = Dissy.BOT.getGuildByID(Long.parseUnsignedLong(args.guildId));
                        if (guild == null) {
                            throw new IllegalArgumentException("Invalid guild ID.");
                        }
                        return guild.getVoiceChannels().stream()
                                .map(ic -> new DissyChannel(ic.getStringID(), ic.getName()))
                                .collect(toImmutableList());
                    });
        }));
        server.register("dis.selectChannel", RpcEventHandler.typed(SelectChannel.class, (client, args) -> {
            GuildChannels.INSTANCE.setChannel(args.guildId, client.getUserId(), args.channelId);
        }));

        Map<String, DissySub> subscriptions = new ConcurrentHashMap<>();
        server.register("dis.subscribe", RpcEventHandler.typed(String.class, (client, guildId) -> {
            String sessId = client.getId();
            if (subscriptions.containsKey(sessId)) {
                disUnsubscribe(subscriptions.get(sessId));
            }

            DissySub sub = new DissySub(client, guildId);

            String channel = GuildChannels.INSTANCE.getChannel(guildId);
            sub.onNewChannel(NewChannel.create(channel));

            GuildChannels.INSTANCE.events.subscribe(guildId, sub);

            subscriptions.put(client.getId(), sub);
        }));

        server.register("dis.unsubscribe", RpcEventHandler.typed(Void.class, (client, nothing) -> {
            String sessId = client.getId();
            if (subscriptions.containsKey(sessId)) {
                disUnsubscribe(subscriptions.remove(sessId));
            }
        }));

        server.getEvents().register(new Object() {

            @Subscribe
            public void onDisconnect(RpcDisconnect disconnect) {
                DissySub s = subscriptions.remove(disconnect.getClient().getId());
                if (s != null) {
                    disUnsubscribe(s);
                }
            }
        });
    }

    private static final class ApiSkipSong {

        public String guildId;
        public String songId;
    }

    private void setupEvents() {
        server.register("event.skipSong", RpcEventHandler.typed(ApiSkipSong.class, (client, args) -> {
            if (args.songId == null) {
                return;
            }
            GuildQueue.INSTANCE.skipSong(args.guildId, client.getUserId(), args.songId);
        }));
    }

}

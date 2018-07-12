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

import com.google.common.collect.ImmutableList;
import com.google.common.eventbus.Subscribe;

import me.kenzierocks.ourtube.events.AvailableChannelsEvent;
import me.kenzierocks.ourtube.events.ChannelSelectedEvent;
import me.kenzierocks.ourtube.events.SongProgressEvent;
import me.kenzierocks.ourtube.events.SongQueuedEvent;
import me.kenzierocks.ourtube.events.SongSkippedEvent;
import me.kenzierocks.ourtube.events.SongStartedEvent;
import me.kenzierocks.ourtube.events.SongVolumeEvent;
import me.kenzierocks.ourtube.guildchannels.GuildChannels;
import me.kenzierocks.ourtube.guildqueue.GuildQueue;
import me.kenzierocks.ourtube.guildvol.GuildVolume;
import me.kenzierocks.ourtube.response.RawChannel;
import me.kenzierocks.ourtube.response.RawGuild;
import me.kenzierocks.ourtube.rpc.RpcClient;
import me.kenzierocks.ourtube.rpc.RpcDisconnect;
import me.kenzierocks.ourtube.rpc.RpcEventHandler;
import me.kenzierocks.ourtube.rpc.RpcRegistry;
import me.kenzierocks.ourtube.songprogress.SongProgress;
import me.kenzierocks.ourtube.songprogress.SongProgressMap;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.Permissions;

public class OurTubeApi {

    private final RpcRegistry server;

    public OurTubeApi(RpcRegistry socketIONamespace) {
        this.server = socketIONamespace;
    }

    public void configure() {
        setupGuildEvents();
        setupGuildControls();
        setupDiscordQueries();
        setupYoutubeQueries();
    }

    private static final class GuildEventsSubscription {

        private final RpcClient client;

        GuildEventsSubscription(RpcClient client) {
            this.client = client;
        }

        @Subscribe
        public void onQueue(SongQueuedEvent event) {
            client.callFunction("guildEvents.songQueued", event);
        }

        @Subscribe
        public void onSkipped(SongSkippedEvent event) {
            client.callFunction("guildEvents.songSkipped", event);
        }

        @Subscribe
        public void onStarted(SongStartedEvent event) {
            client.callFunction("guildEvents.songStarted", event);
        }

        @Subscribe
        public void onNewProgress(SongProgressEvent event) {
            client.callFunction("guildEvents.songProgressed", event);
        }

        @Subscribe
        public void onSetVolume(SongVolumeEvent event) {
            client.callFunction("guildEvents.volumeChanged", event);
        }

        @Subscribe
        public void onChannelSelect(ChannelSelectedEvent event) {
            client.callFunction("guildEvents.channelSelected", event);
        }

    }

    private void guildEventsUnsubscribe(GuildEventsSubscription subscription) {
        subscription.client.getGuilds().stream()
                .map(IGuild::getStringID)
                .forEach(guildId -> Events.OUR_EVENTS.unsubscribe(guildId, subscription));
    }

    private void setupGuildEvents() {
        Map<String, GuildEventsSubscription> subscriptions = new ConcurrentHashMap<>();
        server.register("guildEvents.subscribe", RpcEventHandler.typed(Void.class, (client, none) -> {
            String sessId = client.getId();
            if (subscriptions.containsKey(sessId)) {
                guildEventsUnsubscribe(subscriptions.get(sessId));
            }

            GuildEventsSubscription subs = new GuildEventsSubscription(client);
            client.getGuilds().stream()
                    .map(IGuild::getStringID)
                    .forEach(guildId -> {
                        Events.OUR_EVENTS.subscribe(guildId, subs);

                        // emit the entire queue for this guildId to the client
                        emitQueues(guildId, subs);

                        emitProgress(guildId, subs);

                        emitVolume(guildId, subs);

                        emitAvailableChannels(guildId, client);
                    });
            subscriptions.put(sessId, subs);
        }));
        server.register("guildEvents.unsubscribe", RpcEventHandler.typed(Void.class, (client, nothing) -> {
            String sessId = client.getId();
            if (subscriptions.containsKey(sessId)) {
                guildEventsUnsubscribe(subscriptions.remove(sessId));
            }
        }));

        server.getEvents().register(new Object() {

            @Subscribe
            public void onDisconnect(RpcDisconnect disconnect) {
                GuildEventsSubscription s = subscriptions.remove(disconnect.getClient().getId());
                if (s != null) {
                    guildEventsUnsubscribe(s);
                }
            }
        });
    }

    private void emitVolume(String guildId, GuildEventsSubscription subs) {
        float volume = GuildVolume.INSTANCE.getVolume(guildId);
        subs.onSetVolume(SongVolumeEvent.create(guildId, volume));
    }

    private void emitProgress(String guildId, GuildEventsSubscription subs) {
        SongProgress progress = SongProgressMap.INSTANCE.getProgress(guildId);
        if (progress != null) {
            subs.onNewProgress(SongProgressEvent.from(guildId, progress));
        }
    }

    private void emitQueues(String guildId, GuildEventsSubscription subs) {
        Stream.concat(
                Stream.of(Dissy.getPlayer(guildId).getPlayingTrack()),
                Dissy.getScheduler(guildId).allTracksStream())
                .map(tr -> SongQueuedEvent.fromTrack(guildId, tr))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .forEach(subs::onQueue);
    }

    private void emitAvailableChannels(String guildId, RpcClient client) {
        ImmutableList<RawChannel> channels = Dissy.BOT.getGuildByID(Long.parseUnsignedLong(guildId))
                .getVoiceChannels()
                .stream()
                .filter(vc -> vc.getModifiedPermissions(Dissy.BOT.getOurUser()).contains(Permissions.VOICE_SPEAK))
                .map(RawChannel::fromD4jChannel)
                .collect(toImmutableList());
        client.callFunction("guildEvents.availableChannels", AvailableChannelsEvent.create(guildId, channels));
    }

    private static final class QueueSongsArgs {

        public String guildId;
        public String songUrl;
    }

    private static final class SkipSongArgs {

        public String guildId;
        public String queueId;
    }

    private static final class SetVolumeArgs {

        public String guildId;
        public float volume;
    }

    private static final class GetVoiceChannelsArgs {

        public String guildId;
        public String callbackName;
    }

    private static final class SelectChannelArgs {

        public String guildId;
        public String channelId;
    }

    private void setupGuildControls() {
        server.register("guild.queue", RpcEventHandler.typed(QueueSongsArgs.class, (client, queueSongs) -> {
            GuildQueue.INSTANCE.queueSongs(queueSongs.guildId, client.getUserId(), queueSongs.songUrl);
        }));
        server.register("guild.skipSong", RpcEventHandler.typed(SkipSongArgs.class, (client, args) -> {
            if (args.queueId == null) {
                return;
            }
            GuildQueue.INSTANCE.skipSong(args.guildId, client.getUserId(), args.queueId);
        }));
        server.register("guild.setVolume", RpcEventHandler.typed(SetVolumeArgs.class, (client, setVolume) -> {
            GuildVolume.INSTANCE.setVolume(setVolume.guildId, client.getUserId(), setVolume.volume);
        }));
        server.register("guild.channels", RpcEventHandler.typed(GetVoiceChannelsArgs.class, (client, args) -> {
            AsyncService.asyncResponse(client, args.callbackName,
                    () -> {
                        IGuild guild = Dissy.BOT.getGuildByID(Long.parseUnsignedLong(args.guildId));
                        if (guild == null) {
                            throw new IllegalArgumentException("Invalid guild ID.");
                        }
                        return guild.getVoiceChannels().stream()
                                .map(ic -> new RawChannel(ic.getStringID(), ic.getName()))
                                .collect(toImmutableList());
                    });
        }));
        server.register("guild.selectChannel", RpcEventHandler.typed(SelectChannelArgs.class, (client, args) -> {
            GuildChannels.INSTANCE.setChannel(args.guildId, client.getUserId(), args.channelId);
        }));
    }

    private static final class GetGuildsArgs {

        public String callbackName;
    }

    private static final class GetNicknameArgs {

        public String guildId;
        public String userId;
        public String callbackName;
    }

    private void setupDiscordQueries() {
        server.register("discord.myGuilds", RpcEventHandler.typed(GetGuildsArgs.class, (client, args) -> {
            AsyncService.asyncResponse(client, args.callbackName,
                    () -> client.getGuilds()
                            .stream()
                            .map(RawGuild::fromD4jGuild)
                            .collect(toImmutableList()));
        }));
        server.register("discord.userNickname", RpcEventHandler.typed(GetNicknameArgs.class, (client, args) -> {
            AsyncService.asyncResponse(client, args.callbackName,
                    () -> Dissy.getNicknameForUserInGuild(args.guildId, args.userId));
        }));
    }

    private static final class GetSongDataArgs {

        public String dataId;
        public String callbackName;
    }

    private void setupYoutubeQueries() {
        server.register("youtube.songData", RpcEventHandler.typed(GetSongDataArgs.class, (client, data) -> {
            AsyncService.asyncResponse(client, data.callbackName, YoutubeAccess.INSTANCE.getVideoDataCached(data.dataId));
        }));
    }

}

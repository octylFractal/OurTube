/*
 * This file is part of OurTube-serverj, licensed under the MIT License (MIT).
 *
 * Copyright (c) Octavia Togami (octylFractal) <https://octyl.net>
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

import com.google.common.base.Throwables;
import com.google.common.eventbus.Subscribe;
import com.google.common.io.Resources;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import discord4j.core.DiscordClient;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.event.domain.guild.GuildCreateEvent;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Channel;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.GuildChannel;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.MessageChannel;
import discord4j.core.object.entity.VoiceChannel;
import discord4j.core.object.util.Snowflake;
import discord4j.voice.VoiceConnection;
import me.kenzierocks.ourtube.guildchannels.GuildChannels;
import me.kenzierocks.ourtube.guildchannels.NewChannel;
import me.kenzierocks.ourtube.lava.OurTubeAudioProvider;
import me.kenzierocks.ourtube.lava.OurTubeAudioSourceMananger;
import me.kenzierocks.ourtube.lava.TrackScheduler;

import javax.annotation.Nullable;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class Dissy {

    public static final DiscordClient BOT = new DiscordClientBuilder(Environment.DISCORD_TOKEN)
        .build();

    private static final AudioPlayerManager manager = new DefaultAudioPlayerManager();

    static {
        AudioSourceManagers.registerLocalSource(manager);
        manager.registerSourceManager(new OurTubeAudioSourceMananger());
    }

    public static AudioPlayerManager getManager() {
        return manager;
    }

    private static final Map<Snowflake, AudioPlayer> guildPlayers = new ConcurrentHashMap<>();

    public static AudioPlayer getPlayer(Snowflake guildId) {
        return guildPlayers.computeIfAbsent(guildId, k -> manager.createPlayer());
    }

    private static final Map<Snowflake, TrackScheduler> guildSchedulers = new ConcurrentHashMap<>();

    public static TrackScheduler getScheduler(Snowflake guildId) {
        return guildSchedulers.computeIfAbsent(guildId, k -> new TrackScheduler(k, getPlayer(k)));
    }

    private static final Map<Snowflake, OurTubeAudioProvider> guildProviders = new ConcurrentHashMap<>();

    public static OurTubeAudioProvider getProvider(Snowflake guildId) {
        return guildProviders
            .computeIfAbsent(guildId, k -> new OurTubeAudioProvider(k, getPlayer(k)));
    }

    private static final Map<Snowflake, ActiveConnection> guildConnections = new ConcurrentHashMap<>();

    @Nullable
    public static ActiveConnection getConnection(Snowflake guildId) {
        return guildConnections.get(guildId);
    }

    private static final Object startupFlag = new Object();

    static {
        URL startup = Resources.getResource("defaultsounds/winXpStart.mp3");
        TempFileCache
            .cacheData("startup-xp", () -> Resources.asByteSource(startup).openBufferedStream());
    }

    public static CompletableFuture<AudioTrack> loadItem(String identifier) {
        CompletableFuture<AudioTrack> future = new CompletableFuture<>();
        manager.loadItem(identifier,
            new AudioLoadResultHandler() {

                @Override
                public void trackLoaded(AudioTrack track) {
                    future.complete(track);
                }

                @Override
                public void playlistLoaded(AudioPlaylist playlist) {
                    future.complete(playlist.getTracks().get(0));
                }

                @Override
                public void noMatches() {
                    future.completeExceptionally(new IllegalStateException("Missing sound loader"));
                }

                @Override
                public void loadFailed(FriendlyException exception) {
                    future.completeExceptionally(exception);
                }
            });
        return future;
    }

    public static <T> T getCfEasily(CompletableFuture<T> cf) {
        try {
            return cf.get();
        } catch (ExecutionException e) {
            Throwable t = e.getCause();
            Throwables.throwIfUnchecked(t);
            throw new RuntimeException(t);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    private static AudioTrack getStartupSound() {
        CompletableFuture<AudioTrack> future = loadItem(
            TempFileCache.getCachedData("startup-xp").toString());
        AudioTrack audioTrack = getCfEasily(future);
        audioTrack.setUserData(startupFlag);
        return audioTrack;
    }

    public static String getNameForUserInGuild(Snowflake guildId, Snowflake userId) {
        return BOT.getMemberById(guildId, userId)
            .map(Member::getDisplayName)
            .block();
    }

    static {
        BOT.getEventDispatcher().on(MessageCreateEvent.class).subscribe(event -> {
            Message message = event.getMessage();
            MessageChannel channel = message.getChannel().block();
            if (channel == null || channel instanceof GuildChannel ||
                message.getAuthor()
                    .flatMap(user -> BOT.getSelfId()
                        .filter(id -> !id.equals(user.getId())))
                    .isPresent()) {
                return;
            }
            channel.createMessage("no u");
        });
        BOT.getEventDispatcher().on(GuildCreateEvent.class).subscribe(guildSubscriber());
    }

    private static Consumer<GuildCreateEvent> guildSubscriber() {
        return event -> {
            Guild guild = event.getGuild();
            Snowflake guildId = guild.getId();
            GuildChannels.INSTANCE.events.subscribe(guildId, new Object() {

                @Subscribe
                public void onNewChannel(NewChannel newChannel) {
                    if (newChannel.getChannelId() == null) {
                        return;
                    }
                    Channel channel = guild.getChannelById(newChannel.getChannelId()).block();
                    if (!(channel instanceof VoiceChannel)) {
                        return;
                    }
                    VoiceChannel voiceChannel = (VoiceChannel) channel;
                    voiceChannel
                        .join(spec -> spec.setProvider(getProvider(guildId)))
                        .subscribe(voiceConnection -> {
                            guildConnections.put(guildId, new ActiveConnection(
                                voiceChannel, voiceConnection
                            ));
                            getScheduler(guildId).nextTrack(true);
                        });
                    TrackScheduler sch = getScheduler(guildId);
                    sch.getAccessLock().lock();
                    try {
                        if (sch.allTracksStream()
                            .map(AudioTrack::getUserData)
                            .allMatch(Predicate.isEqual(startupFlag))) {
                            // just start up sounds. queue another!
                            sch.addTrack(BOT.getSelfId().orElseThrow(), getStartupSound());
                        }
                    } finally {
                        sch.getAccessLock().unlock();
                    }
                }
            });
        };
    }

    private Dissy() {
    }

}

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

import java.net.URL;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.function.Predicate;

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

import me.kenzierocks.ourtube.guildchannels.GuildChannels;
import me.kenzierocks.ourtube.guildchannels.NewChannel;
import me.kenzierocks.ourtube.guildqueue.GuildQueue;
import me.kenzierocks.ourtube.lava.AudioProvider;
import me.kenzierocks.ourtube.lava.OurTubeAudioSourceMananger;
import me.kenzierocks.ourtube.lava.TrackScheduler;
import sx.blah.discord.api.ClientBuilder;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.api.events.IListener;
import sx.blah.discord.handle.impl.events.guild.GuildCreateEvent;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageEvent;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IVoiceChannel;

public class Dissy {

    public static final IDiscordClient BOT = new ClientBuilder()
            .withToken(Environment.DISCORD_TOKEN)
            .registerListener(guildSubscriber())
            .registerListener(GuildQueue.INSTANCE)
            .login();

    private static final AudioPlayerManager manager = new DefaultAudioPlayerManager();
    static {
        AudioSourceManagers.registerLocalSource(manager);
        manager.registerSourceManager(new OurTubeAudioSourceMananger());
    }

    public static AudioPlayerManager getManager() {
        return manager;
    }

    private static final Map<String, AudioPlayer> guildPlayers = new ConcurrentHashMap<>();

    public static AudioPlayer getPlayer(String guildId) {
        return guildPlayers.computeIfAbsent(guildId, k -> manager.createPlayer());
    }

    private static final Map<String, TrackScheduler> guildSchedulers = new ConcurrentHashMap<>();

    public static TrackScheduler getScheduler(String guildId) {
        return guildSchedulers.computeIfAbsent(guildId, k -> new TrackScheduler(k, getPlayer(k)));
    }

    private static final Map<String, AudioProvider> guildProviders = new ConcurrentHashMap<>();

    public static AudioProvider getProvider(String guildId) {
        return guildProviders.computeIfAbsent(guildId, k -> new AudioProvider(k, getPlayer(k)));
    }

    private static final Object startupFlag = new Object();

    static {
        URL startup = Resources.getResource("defaultsounds/winXpStart.mp3");
        TempFileCache.cacheData("startup-xp", () -> Resources.asByteSource(startup).openBufferedStream());
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
        CompletableFuture<AudioTrack> future = loadItem(TempFileCache.getCachedData("startup-xp").toString());
        AudioTrack audioTrack = getCfEasily(future);
        audioTrack.setUserData(startupFlag);
        return audioTrack;
    }

    public static String getNameForUserInGuild(String guildId, String userId) {
        IGuild guild = BOT.getGuildByID(Long.parseUnsignedLong(guildId));
        return guild.getUserByID(Long.parseUnsignedLong(userId))
                .getDisplayName(guild);
    }

    static {
        BOT.getDispatcher().registerListener(new Object() {

            @EventSubscriber
            public void onMessage(MessageEvent event) {
                if (!event.getChannel().isPrivate() || event.getAuthor().getLongID() == BOT.getOurUser().getLongID()) {
                    return;
                }
                event.getChannel().sendMessage("no u");
            }
        });
    }

    private static IListener<GuildCreateEvent> guildSubscriber() {
        return event -> {
            IGuild guild = event.getGuild();
            String guildId = guild.getStringID();
            IVoiceChannel connected = guild.getConnectedVoiceChannel();
            if (connected != null) {
                connected.leave();
            }
            guild.getAudioManager().setAudioProvider(getProvider(guildId));
            GuildChannels.INSTANCE.events.subscribe(guildId, new Object() {

                @Subscribe
                public void onNewChannel(NewChannel newChannel) {
                    if (newChannel.getChannelId() == null) {
                        IVoiceChannel conn = guild.getConnectedVoiceChannel();
                        if (conn != null) {
                            conn.leave();
                        }
                        return;
                    }
                    long cId = Long.parseUnsignedLong(newChannel.getChannelId());
                    guild.getVoiceChannelByID(cId).join();
                    TrackScheduler sch = getScheduler(guildId);
                    sch.getAccessLock().lock();
                    try {
                        if (sch.allTracksStream()
                                .map(AudioTrack::getUserData)
                                .allMatch(Predicate.isEqual(startupFlag))) {
                            // just start up sounds. queue another!
                            sch.addTrack(BOT.getOurUser().getStringID(), getStartupSound());
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

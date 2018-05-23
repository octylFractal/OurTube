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

import java.io.IOException;
import java.util.List;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import com.google.common.eventbus.Subscribe;
import com.google.common.io.Resources;

import me.kenzierocks.ourtube.guildchannels.GuildChannels;
import me.kenzierocks.ourtube.guildchannels.NewChannel;
import me.kenzierocks.ourtube.guildqueue.GuildQueue;
import sx.blah.discord.api.ClientBuilder;
import sx.blah.discord.api.IDiscordClient;
import sx.blah.discord.api.events.EventSubscriber;
import sx.blah.discord.api.events.IListener;
import sx.blah.discord.handle.impl.events.guild.GuildCreateEvent;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageEvent;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IVoiceChannel;
import sx.blah.discord.util.audio.AudioPlayer.Track;

public class Dissy {

    public static final IDiscordClient BOT = new ClientBuilder()
            .withToken(Environment.DISCORD_TOKEN)
            .registerListener(guildSubscriber())
            .registerListener(GuildQueue.INSTANCE)
            .login();

    private static AudioInputStream getStartupSound() {
        try {
            return AudioSystem.getAudioInputStream(Resources.getResource("defaultsounds/winXpStart.mp3"));
        } catch (UnsupportedAudioFileException | IOException e) {
            throw new RuntimeException(e);
        }
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
            GuildChannels.INSTANCE.events.subscribe(guildId, new Object() {

                @Subscribe
                public void onNewChannel(NewChannel newChannel) {
                    if (newChannel.getChannelId() == null) {
                        IVoiceChannel conn = guild.getConnectedVoiceChannel();
                        conn.leave();
                        return;
                    }
                    long cId = Long.parseUnsignedLong(newChannel.getChannelId());
                    guild.getVoiceChannelByID(cId).join();
                    List<Track> playlist = GuildQueue.getPlayer(guildId).getPlaylist();
                    if (playlist.isEmpty()) {
                        playlist.add(new Track(getStartupSound()));
                    }
                }
            });
        };
    }

    private Dissy() {
    }

}

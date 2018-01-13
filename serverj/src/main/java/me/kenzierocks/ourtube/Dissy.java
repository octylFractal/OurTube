package me.kenzierocks.ourtube;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.sound.sampled.AudioInputStream;

import com.google.common.eventbus.Subscribe;

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

public class Dissy {

    public static final IDiscordClient BOT = new ClientBuilder()
            .withToken(Environment.DISCORD_TOKEN)
            .registerListener(guildSubscriber())
            .login();

    public static final Events events = new Events("dissy");

    private static final class OurTubePipe {

        private final String guildId;
        private final AudioPlayer player;
        private final Lock waitLock = new ReentrantLock();
        private boolean waiting;

        public OurTubePipe(String guildId, AudioPlayer player) {
            this.guildId = guildId;
            this.player = player;
            BOT.getDispatcher().registerListener(new Object() {

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
                    GuildQueue.INSTANCE.popSong(guildId);
                    // don't block the threads, idiot!
                    AsyncService.GENERIC.execute(() -> playNext());
                }
            });
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
            AudioInputStream pcmData = YoutubeStreams.newStream(songId);
            player.queue(new Track(pcmData));
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
                    BOT.getDispatcher().unregisterListener(this);
                    AudioPlayer player = AudioPlayer.getAudioPlayerForGuild(guild);

                    OurTubePipe pipe = new OurTubePipe(guild.getStringID(), player);
                    BOT.getDispatcher().registerListener(new Object() {

                        @EventSubscriber
                        public void onLeaveChannel(UserVoiceChannelLeaveEvent leaveEvent) {
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

package me.kenzierocks.ourtube.guildchannels;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import me.kenzierocks.ourtube.Events;

public enum GuildChannels {
    INSTANCE;

    public final Events events = new Events("SongProgress");

    private final Map<String, String> progressMap = new ConcurrentHashMap<>();

    @Nullable
    public String getChannel(String guildId) {
        return progressMap.get(guildId);
    }

    public void setChannel(String guildId, String channelId) {
        String old = progressMap.put(guildId, channelId);
        if (!Objects.equals(channelId, old)) {
            events.post(guildId, NewChannel.create(channelId));
        }
    }

}

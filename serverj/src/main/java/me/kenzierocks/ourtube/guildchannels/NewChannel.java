package me.kenzierocks.ourtube.guildchannels;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class NewChannel {

    public static NewChannel create(String channelId) {
        return new AutoValue_NewChannel(channelId);
    }

    NewChannel() {
    }

    public abstract String getChannelId();

}

package me.kenzierocks.ourtube.guildqueue;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class PushSong {

	public static PushSong create(String songId) {
		return new AutoValue_PushSong(songId);
	}

	PushSong() {
	}

	public abstract String getYoutubeId();

}

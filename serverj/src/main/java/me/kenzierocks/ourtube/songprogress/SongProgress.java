package me.kenzierocks.ourtube.songprogress;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class SongProgress {

	SongProgress() {
	}

	public abstract String getSongId();

	public abstract double getProgress();

}

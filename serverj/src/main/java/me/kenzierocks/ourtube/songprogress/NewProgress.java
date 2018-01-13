package me.kenzierocks.ourtube.songprogress;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class NewProgress {

	public static NewProgress create(SongProgress progress) {
		return new AutoValue_NewProgress(progress);
	}

	NewProgress() {
	}

	public abstract SongProgress getProgress();

}

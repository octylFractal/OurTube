package me.kenzierocks.ourtube.events;

public class SkipSong {

	private static final SkipSong INSTANCE = new SkipSong();

	public static SkipSong create() {
		return INSTANCE;
	}

	private SkipSong() {
	}

}

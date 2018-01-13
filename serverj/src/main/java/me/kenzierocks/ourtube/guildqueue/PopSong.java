package me.kenzierocks.ourtube.guildqueue;

public class PopSong {

	private static final PopSong INSTANCE = new PopSong();

	public static PopSong create() {
		return INSTANCE;
	}

	private PopSong() {
	}

}

package me.kenzierocks.ourtube;

import static com.google.common.base.Preconditions.checkNotNull;

// worlds worst env class
public class Environment {

	public static final String YOUTUBE_API_KEY = System.getenv("YOUTUBE_API_KEY");
	public static final String DISCORD_TOKEN = System.getenv("DISCORD_TOKEN");
	static {
        checkNotNull(YOUTUBE_API_KEY, "No YOUTUBE_API_KEY provided.");
        checkNotNull(DISCORD_TOKEN, "No DISCORD_TOKEN provided.");
	}

}

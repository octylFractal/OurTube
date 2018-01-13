package me.kenzierocks.ourtube.songprogress;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import me.kenzierocks.ourtube.Events;

public enum SongProgressMap {
    INSTANCE;

    public final Events events = new Events("SongProgress");

    private final Map<String, SongProgress> progressMap = new ConcurrentHashMap<>();

    @Nullable
    public SongProgress getProgress(String guildId) {
        return progressMap.get(guildId);
    }

    public void setProgress(String guildId, SongProgress progress) {
        progressMap.put(guildId, progress);
        events.post(guildId, NewProgress.create(progress));
    }

}

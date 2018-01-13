package me.kenzierocks.ourtube.guildqueue;

import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import me.kenzierocks.ourtube.Events;

public enum GuildQueue {
    INSTANCE;

    public final Events events = new Events("GuildQueue");

    private final Map<String, GQ> songQueues = new ConcurrentHashMap<>();

    public void useQueue(String guildId, Consumer<Deque<String>> user) {
        songQueues.computeIfAbsent(guildId, k -> new GQ()).useQueue(user);
    }

    public void queueSong(String guildId, String songId) {
        useQueue(guildId, q -> q.addLast(songId));
        events.post(guildId, PushSong.create(songId));
    }

    public void popSong(String guildId) {
        useQueue(guildId, q -> {
            if (q.isEmpty()) {
                return;
            }
            q.removeFirst();
            events.post(guildId, PopSong.create());
        });
    }

}

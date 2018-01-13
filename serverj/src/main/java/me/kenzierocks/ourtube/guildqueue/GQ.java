package me.kenzierocks.ourtube.guildqueue;

import java.util.Deque;
import java.util.LinkedList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

final class GQ {

    private final Lock lock = new ReentrantLock();
    private final LinkedList<String> queue = new LinkedList<>();

    public void useQueue(Consumer<Deque<String>> user) {
        lock.lock();
        try {
            user.accept(queue);
        } finally {
            lock.unlock();
        }
    }

}

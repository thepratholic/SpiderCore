package org.spidercore;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

public class URLStore {

    private final ConcurrentHashMap<String, Boolean> visitedUrl = new ConcurrentHashMap<>();

    private final BlockingQueue<String> urlQueue = new LinkedBlockingQueue<>();

    private final DatabaseManager databaseManager;

    public URLStore(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public boolean addUrl(String url, int depth) {

        if (visitedUrl.putIfAbsent(url, true) == null) {

            databaseManager.insertUrl(url, depth);
            urlQueue.offer(url);

            return true;
        }

        return false;
    }

    public String getNextUrl() {
        return urlQueue.poll();
    }

    public boolean isEmptyQueue() {
        return urlQueue.isEmpty();
    }
}

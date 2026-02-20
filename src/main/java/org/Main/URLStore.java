package org.Main;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

public class URLStore {
    /* this ConcurrentHashMap is ued because it is thread safe, if we use normal HashMap, then
    *  most of the threads will start the fight and may read the wrong data, can also corrupt the data.
    *  ConcurrentHashMap itself handles the threads and it locks small segment of it where threads have to work.*/
    private final ConcurrentHashMap<String, Boolean> visitedUrl = new ConcurrentHashMap<>();

    /* This queue is the “line” where URLs wait to be crawled.
    *  This queue behaves smart in under heavy multithreading.
    *  It is thread safe, and handles producer-consumer model.
    *  LinkedBlockingQueue is used, coz it is fast and unbounded for any type of limit.*/
    private final BlockingQueue<String> urlQueue = new LinkedBlockingQueue<>();


    // Some behaviours to store and manage URLs
    public boolean addUrl(String url) {

        // putIfAbsent() returns true if this url is already present in map.
        // else returns null when it puts for first time.
        if(visitedUrl.putIfAbsent(url, true) == null) {
            // offer method is fit because we are using non-restricted size of queue, so it will not throw any exception.
            urlQueue.offer(url);
            return true;
        }
        return false;
    }

    // we are returning the nextt url from the queue.
    public String getNextUrl() {
        // poll() is used to get the front item from the queue
        return urlQueue.poll();
    }

    // to check wheather tthe queue is empty or not.
    public boolean isEmptyQueue() {
        return urlQueue.isEmpty();
    }
}

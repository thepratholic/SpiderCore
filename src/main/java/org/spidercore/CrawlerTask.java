package org.spidercore;

import java.util.Set;
import java.util.concurrent.Phaser;

public class CrawlerTask implements Runnable {
    private final URLStore urlStore;
    private final URLFetcher urlFetcher;
    private final int maxDepth;
    private final int currentDepth;
    private final Phaser phaser;
    private final DatabaseManager databaseManager;

    public CrawlerTask(URLStore urlStore, URLFetcher urlFetcher, int maxDepth, int currentDepth, Phaser phaser, DatabaseManager databaseManager) {
        this.urlStore = urlStore;
        this.urlFetcher = urlFetcher;
        this.maxDepth = maxDepth;
        this.currentDepth = currentDepth;
        this.phaser = phaser;
        this.databaseManager = databaseManager;
    }

    @Override
    public void run() {
        try {
            // We are printing current thread name and that thread is using which url, that too.
            String url = urlStore.getNextUrl();
            System.out.println(Thread.currentThread().getName() + " " + url);

            // what if url is null (means empty) or currentDepth has been exceeded, then we should return
            if(url == null || this.currentDepth > this.maxDepth) {
                return;
            }

            // We are getting the links url of the current Web page by passing url
            Set<String> links = urlFetcher.fecthLinks(url);

            for(String link : links) {
                if(urlStore.addUrl(link, this.currentDepth)) {
                    phaser.register(); // We are telling to the phaser that, we have added one url , so now invoke one more task and increment the count by 1 as well.

                    // Now this method will get invoke and then it will be assigned to new thread in the thread pool
                    WebCrawler.submitTask(urlStore, urlFetcher, currentDepth + 1, maxDepth, databaseManager);
                }
            }
        }
        catch (Exception e) {
            System.out.println("Error Occurred!!");
        }
        finally {
            // we have to tell to phaser that this current thread has been done with it's task, and now decrement the count of task.
            phaser.arriveAndDeregister();
        }
    }
}

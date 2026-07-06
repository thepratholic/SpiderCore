package org.spidercore;

import java.util.concurrent.Phaser;

public class CrawlerTask implements Runnable {

    private final URLStore urlStore;
    private final URLFetcher urlFetcher;
    private final int maxDepth;
    private final int currentDepth;
    private final Phaser phaser;

    private final DatabaseManager databaseManager;
    private final CrawlerStats crawlerStats;

    public CrawlerTask(URLStore urlStore, URLFetcher urlFetcher, int maxDepth, int currentDepth, Phaser phaser, DatabaseManager databaseManager, CrawlerStats crawlerStats) {
        this.urlStore = urlStore;
        this.urlFetcher = urlFetcher;
        this.maxDepth = maxDepth;
        this.currentDepth = currentDepth;
        this.phaser = phaser;
        this.databaseManager = databaseManager;
        this.crawlerStats = crawlerStats;
    }

    @Override
    public void run() {
        String url = null;

        try {
            url = urlStore.getNextUrl();

            System.out.println(Thread.currentThread().getName() + " crawling: " + url);

            if (url == null || this.currentDepth > this.maxDepth) {
                return;
            }

            CrawlResult result = urlFetcher.fetchLinks(url);

            databaseManager.updateUrlStatus(url, "visited");

            crawlerStats.incrementCrawled();

            int urlId = databaseManager.getUrlId(url);
            if (urlId != -1) {
                databaseManager.saveCrawledData(urlId, result.getTitle(), result.getLinks().size());
            }

            for (String link : result.getLinks()) {

                if (urlStore.addUrl(link, currentDepth + 1)) {
                    WebCrawler.submitTask(urlStore, urlFetcher, currentDepth + 1, maxDepth, databaseManager, crawlerStats);
                }
            }

        } catch (Exception e) {
            if (url != null) {
                databaseManager.updateUrlStatus(url, "failed");
                crawlerStats.incrementFailed();
            }
            System.out.println("Error crawling: " + url + " → " + e.getMessage());

        } finally {
            // System.out.println("PHASER COUNT: " + phaser.getUnarrivedParties()); // debug line
            phaser.arriveAndDeregister();
        }
    }
}

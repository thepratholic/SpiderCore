package org.spidercore;

import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Phaser;

public class WebCrawler {

    private static Phaser phaser;

    private static ExecutorService executorService;

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        System.out.println("Enter your URL:");
        String url = sc.nextLine();

        System.out.println("Enter the Depth of the crawler:");
        final int MAX_DEPTH = sc.nextInt();

        System.out.println("Enter the number of workers:");
        final int MAX_THREADS = sc.nextInt();

        DatabaseManager databaseManager = new DatabaseManager();

        URLStore urlStore = new URLStore(databaseManager);
        URLFetcher urlFetcher = new URLFetcher();
        CrawlerStats crawlerStats = new CrawlerStats();

        phaser = new Phaser(0);

        executorService = Executors.newFixedThreadPool(MAX_THREADS);

        urlStore.addUrl(url, 0);

        long startTime = System.currentTimeMillis();

        submitTask(urlStore, urlFetcher, 0, MAX_DEPTH, databaseManager, crawlerStats);

        phaser.awaitAdvance(phaser.getPhase());

        executorService.shutdown();

        crawlerStats.printReport();

        databaseManager.closeConnection();

        System.out.println("Crawling complete!");
        System.out.println("Time taken: " + (System.currentTimeMillis() - startTime) + "ms");

        sc.close();
    }

    public static void submitTask(URLStore urlStore, URLFetcher urlFetcher, int currentDepth, int maxDepth, DatabaseManager databaseManager, CrawlerStats crawlerStats) {
        phaser.register(); // ek aur task aa raha hai — count badhao
        executorService.submit(new CrawlerTask(urlStore, urlFetcher, maxDepth, currentDepth, phaser, databaseManager, crawlerStats));
    }
}

package org.spidercore;

import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Phaser;

public class WebCrawler {
    // A phaser is basically like a counter, which tracks that how many threads are currently working.
    private static Phaser phaser;

    // ExecutorService is basically is a pool of fixed number of threads
    private static ExecutorService executorService;

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        System.out.println("Enter your URL:"); // Getting the URL
        String url = sc.nextLine();

        System.out.println("Enter the Depth of the crawler:"); // Getting the Depth, as Global
        final int MAX_DEPTH = sc.nextInt();

        System.out.println("Enter the number of workers:"); // Getting the number of Threads
        final int MAX_THREADS = sc.nextInt();

        URLStore urlStore = new URLStore();
        URLFetcher urlFetcher = new URLFetcher();
        phaser = new Phaser(1); // adding 1 at start, because we have to count this main thread as well
        executorService = Executors.newFixedThreadPool(MAX_THREADS); // This means to ready MAX_THREADS workers to work concurrently

        urlStore.addUrl(url); // Starting URL has been inserted to Queue, because to start, their must be atleast one URL

        long startTime = System.currentTimeMillis(); // We are saving start time of Crawling

        submitTask(urlStore, urlFetcher, 0, MAX_DEPTH); // It basically mean that start the thread from depth 0
        phaser.awaitAdvance(phaser.getPhase()); // Here main thread waits until all the crawling tasks get completed

        executorService.shutdown(); // We close the threadpool, so any worker (thread) should not consume any resources

        System.out.println("Time taken : " + (System.currentTimeMillis() - startTime)); // Tells how much time crawling gets compeleted in MS.
        sc.close();
    }

    /* This simply does:
    *  1) Creates new crawler task
    *  2) Submit it to thread pool
    *  3) Then Thread pool assigns this newly created task to some worker which is available. */
    public static void submitTask(URLStore urlStore, URLFetcher urlFetcher, int currentDepth, int maxDepth) {
        executorService.submit(new CrawlerTask(urlStore, urlFetcher, maxDepth, currentDepth, phaser));
    }
}

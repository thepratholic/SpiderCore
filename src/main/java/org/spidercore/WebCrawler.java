package org.spidercore;

import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Phaser;

/*
 * WebCrawler — Ye poore crawler ka "entry point" aur "controller" hai.
 *
 * Kya karta hai:
 * 1. User se input leta hai (URL, depth, threads)
 * 2. Saari dependencies banata hai (DatabaseManager, URLStore, URLFetcher)
 * 3. ThreadPool manage karta hai
 * 4. Phaser se wait karta hai jab tak saara kaam khatam na ho
 *
 * Real world example:
 * Ye ek construction site ka "project manager" hai —
 * Manager kaam nahi karta khud, lekin
 * workers (threads) ko kaam assign karta hai
 * aur tab tak wait karta hai jab tak sab khatam na ho
 */
public class WebCrawler {

    /* Phaser — ek smart counter jo track karta hai
     * ki kitne threads abhi kaam kar rahe hain
     *
     * Jaise school mein attendance hoti hai —
     * jab tak saare students (threads) present hain
     * class (program) khatam nahi hoti */
    private static Phaser phaser;

    /* ExecutorService — fixed size thread pool
     * Jaise ek office mein fixed number of employees hain
     * Kaam aata hai → available employee ko assign hota hai
     * Sab busy hain → kaam queue mein wait karta hai */
    private static ExecutorService executorService;

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        System.out.println("Enter your URL:");
        String url = sc.nextLine();

        System.out.println("Enter the Depth of the crawler:");
        final int MAX_DEPTH = sc.nextInt();

        System.out.println("Enter the number of workers:");
        final int MAX_THREADS = sc.nextInt();

        /* DatabaseManager sabse pehle banao —
         * Kyunki URLStore ko ye chahiye constructor mein
         * Jaise ghar banane se pehle neenv chahiye */
        DatabaseManager databaseManager = new DatabaseManager();

        /* URLStore mein DatabaseManager pass karo —
         * Dependency Injection pattern ye hai
         * URLStore khud DatabaseManager nahi banayega
         * Bahar se milega — loosely coupled code */
        URLStore urlStore = new URLStore(databaseManager);
        URLFetcher urlFetcher = new URLFetcher();
        CrawlerStats crawlerStats = new CrawlerStats();

        /* Phaser(1) — 1 isliye kyunki main thread
         * khud bhi count mein hai
         * Agar 0 se start karein toh main thread
         * immediately aage badh jayega — wait nahi karega */
        phaser = new Phaser(1);

        /* newFixedThreadPool — exactly MAX_THREADS workers ready
         * Isse zyada threads kabhi nahi banenge
         * Resources controlled rehte hain */
        executorService = Executors.newFixedThreadPool(MAX_THREADS);

        /* Depth 0 pass karo — starting URL hai ye
         * Iski depth 0 hai — ghar se 0 kadam door */
        urlStore.addUrl(url, 0);

        long startTime = System.currentTimeMillis();

        /* Pehla task submit karo depth 0 se
         * Ye ek domino effect start karega —
         * Ye task aur tasks submit karega, wo aur karengi */
        submitTask(urlStore, urlFetcher, 0, MAX_DEPTH, databaseManager, crawlerStats);

        /* Main thread yahan ruk jaata hai —
         * Jab tak saare threads apna kaam khatam na karein
         * awaitAdvance() — "sab khatam hone tak ruko" */
        phaser.awaitAdvance(phaser.getPhase());

        /* Thread pool band karo —
         * Agar nahi karein toh threads resources consume karte rahenge
         * Jaise office mein sab chale gaye lekin lights band nahi ki */
        executorService.shutdown();

        crawlerStats.printReport();

        /* DB connection band karo —
         * Open connection resources consume karta hai
         * Jaise phone call khatam hone pe line disconnect karo */
        databaseManager.closeConnection();

        System.out.println("Crawling complete!");
        System.out.println("Time taken: " + (System.currentTimeMillis() - startTime) + "ms");

        sc.close();
    }

    /*
     * submitTask() — Naya crawling task thread pool mein submit karna
     *
     * Kya hota hai andar:
     * 1. Naya CrawlerTask object banta hai
     * 2. executorService isse kisi available thread ko deta hai
     * 3. Thread kaam karta hai — crawl karta hai
     *
     * DatabaseManager bhi pass karna pada ab —
     * Kyunki CrawlerTask ko DB update karna hoga
     * (visited status, crawled data save karna)
     */
    public static void submitTask(URLStore urlStore, URLFetcher urlFetcher,
                                  int currentDepth, int maxDepth,
                                  DatabaseManager databaseManager, CrawlerStats crawlerStats) {
        phaser.register(); // ek aur task aa raha hai — count badhao
        executorService.submit(new CrawlerTask(urlStore, urlFetcher,
                                               maxDepth, currentDepth,
                                               phaser, databaseManager, crawlerStats));
    }
}

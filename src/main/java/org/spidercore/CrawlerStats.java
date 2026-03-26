package org.spidercore;

import java.util.concurrent.atomic.AtomicInteger;

/*
 * CrawlerStats — Crawler ki performance track karne wali class
 *
 * Ye class "Observability" provide karti hai crawler ko —
 * Matlab andar kya ho raha hai wo bahar dikhna chahiye
 *
 * Real world example:
 * Socho ek factory hai —
 * Factory mein dashboard hota hai jo dikhata hai:
 * - Kitne products bane (totalCrawled)
 * - Kitne reject hue (totalFailed)
 * - Production rate (pages/sec)
 * Bina dashboard ke factory blindly kaam karti hai!
 *
 * Ye same cheez hai crawler ke liye — CrawlerStats = dashboard
 */
public class CrawlerStats {

    /* AtomicInteger kyun? —
     * Multiple threads same time pe increment karenge
     * Normal int use karein toh race condition hogi —
     * 2 threads same time pe ++ karein toh value galat ho sakti hai
     *
     * AtomicInteger guarantee karta hai —
     * Ek operation ek hi baar hoga, beech mein interrupt nahi
     * Jaise ATM transaction — ek complete hogi tabhi doosri shuru hogi */
    private final AtomicInteger totalCrawled = new AtomicInteger(0);
    private final AtomicInteger totalFailed = new AtomicInteger(0);

    /* startTime — crawler kab shuru hua
     * System.currentTimeMillis() — epoch time in milliseconds
     * End mein startTime se subtract karenge — total time nikalenge
     * final kyun — ek baar set hoga, change nahi hoga */
    private final long startTime;

    /* Constructor mein startTime set karo —
     * Jab bhi CrawlerStats ka object banega
     * usi waqt timer shuru ho jaayega */
    public CrawlerStats() {
        this.startTime = System.currentTimeMillis();
    }

    /*
     * incrementCrawled() — successfully crawl hue pages ka counter badhao
     * CrawlerTask mein call hoga jab page successfully crawl ho jaye
     *
     * incrementAndGet() — atomically +1 karta hai
     * Thread safe operation hai ye
     */
    public void incrementCrawled() {
        totalCrawled.incrementAndGet();
    }

    /*
     * incrementFailed() — failed pages ka counter badhao
     * CrawlerTask ke catch block mein call hoga
     * Jab bhi koi URL crawl fail ho
     */
    public void incrementFailed() {
        totalFailed.incrementAndGet();
    }

    /*
     * printReport() — Crawling khatam hone pe stats print karo
     *
     * Pages/sec kaise calculate karein?
     * totalCrawled / (totalTime in seconds)
     *
     * Example:
     * 150 pages crawl hue
     * 45 seconds lage
     * 150/45 = 3.33 pages/sec
     */
    public void printReport() {
        /* Total time milliseconds mein —
         * abhi ka time - shuru ka time */
        long totalTimeMs = System.currentTimeMillis() - startTime;

        /* Milliseconds to seconds convert karo —
         * 1000ms = 1 second */
        double totalTimeSec = totalTimeMs / 1000.0;

        /* Pages per second —
         * Agar time 0 ho (bahut fast crawler) toh
         * divide by zero se bachne ke liye check karo */
        double pagesPerSec = totalTimeSec > 0
                ? totalCrawled.get() / totalTimeSec
                : 0;

        /* Report print karo — clean format mein
         * String.format() — numbers ko 2 decimal places mein format karo */
        System.out.println("\n╔════════════════════════════════════╗");
        System.out.println(  "║         CRAWLER STATS REPORT       ║");
        System.out.println(  "╠════════════════════════════════════╣");
        System.out.printf(   "║  Total URLs crawled  : %-10d  ║%n", totalCrawled.get());
        System.out.printf(   "║  Failed URLs         : %-10d  ║%n", totalFailed.get());
        System.out.printf(   "║  Total time          : %-10s  ║%n", String.format("%.1fs", totalTimeSec));
        System.out.printf(   "║  Pages/sec           : %-10s  ║%n", String.format("%.2f", pagesPerSec));
        System.out.println(  "╚════════════════════════════════════╝");
    }
}

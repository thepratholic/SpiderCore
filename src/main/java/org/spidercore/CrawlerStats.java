package org.spidercore;
import java.util.concurrent.atomic.AtomicInteger;

public class CrawlerStats {
    private final AtomicInteger totalCrawled = new AtomicInteger(0);
    private final AtomicInteger totalFailed = new AtomicInteger(0);

    private final long startTime;
    public CrawlerStats() {
        this.startTime = System.currentTimeMillis();
    }

    public void incrementCrawled() {
        totalCrawled.incrementAndGet();
    }

    public void incrementFailed() {
        totalFailed.incrementAndGet();
    }

    public void printReport() {
        long totalTimeMs = System.currentTimeMillis() - startTime;

        double totalTimeSec = totalTimeMs / 1000.0;

        double pagesPerSec = totalTimeSec > 0 ? totalCrawled.get() / totalTimeSec : 0;

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

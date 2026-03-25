package org.spidercore;

import java.util.Set;
import java.util.concurrent.Phaser;

/*
 * CrawlerTask — Ye ek single crawling job hai jo ek thread execute karta hai.
 *
 * Ye class Runnable implement karta hai — matlab ye ek "task" hai
 * jo thread pool mein submit hota hai aur koi bhi available
 * thread isse execute kar sakta hai.
 *
 * Real world example:
 * Socho ek delivery boy hai —
 * URLStore = orders ki list (kaunsa deliver karna hai)
 * URLFetcher = delivery karna (actual kaam)
 * DatabaseManager = delivery confirm karna register mein
 * Phaser = office ko batana "meri delivery complete hui"
 */
public class CrawlerTask implements Runnable {

    private final URLStore urlStore;
    private final URLFetcher urlFetcher;
    private final int maxDepth;
    private final int currentDepth;
    private final Phaser phaser;

    /* DatabaseManager add kiya — ab ye task
     * crawl karne ke baad DB mein save karega:
     * 1. URL ka status update karega (pending → visited/failed)
     * 2. Page ka data save karega (title, links count) */
    private final DatabaseManager databaseManager;

    public CrawlerTask(URLStore urlStore, URLFetcher urlFetcher,
                       int maxDepth, int currentDepth,
                       Phaser phaser, DatabaseManager databaseManager) {
        this.urlStore = urlStore;
        this.urlFetcher = urlFetcher;
        this.maxDepth = maxDepth;
        this.currentDepth = currentDepth;
        this.phaser = phaser;
        this.databaseManager = databaseManager;
    }

    @Override
    public void run() {
        String url = null;

        try {
            /* Queue se agla URL uthao — ye thread ka kaam hai
             * poll() — agar queue empty ho toh null return karta hai
             * wait nahi karta — non-blocking hai */
            url = urlStore.getNextUrl();

            System.out.println(Thread.currentThread().getName() + " crawling: " + url);

            /* 2 conditions pe kaam band karo:
             * 1. url null hai — queue empty thi, koi kaam nahi
             * 2. currentDepth > maxDepth — BFS ki limit cross ho gayi
             *
             * Jaise delivery boy ko bola — sirf 2km tak deliver karo
             * 2km se door order aaya → mat jao */
            if (url == null || this.currentDepth > this.maxDepth) {
                return;
            }

            /* Actual crawling — URLFetcher se links fetch karo
             * Ye network call hai — time lag sakta hai
             * Isliye ye thread pool mein hai — main thread block nahi hoga */
            Set<String> links = urlFetcher.fetchLinks(url);

            /* Page crawl hua successfully —
             * DB mein status update karo pending → visited
             * Crash recovery ke liye zaroori hai ye */
            databaseManager.updateUrlStatus(url, "visited");

            /* Page ka data DB mein save karo —
             * url ka id dhundho pehle (foreign key ke liye)
             * phir title aur links count save karo
             *
             * Abhi tera code sirf print karta tha:
             * System.out.println(document.text()) — data gayab ho jaata tha
             * Ab permanently DB mein save hoga ✅ */
            int urlId = databaseManager.getUrlId(url);
            if (urlId != -1) {
                /* fetchLinks() se title access nahi hota directly
                 * isliye abhi links.size() se count save karenge
                 * title ke liye URLFetcher modify karenge aage */
                databaseManager.saveCrawledData(urlId, "N/A", links.size());
            }

            /* Har link ke liye check karo —
             * Naya hai? → add karo aur naya task submit karo
             * Already visited? → skip karo
             *
             * Ye BFS ka core logic hai —
             * Current level ke saare links process karo
             * phir next level pe jao */
            for (String link : links) {

                /* addUrl() — 3 kaam karta hai:
                 * 1. RAM cache check (fast duplicate check)
                 * 2. DB mein insert (permanent save)
                 * 3. Queue mein daalo (buffer)
                 * true return hoga sirf naye URL pe */
                if (urlStore.addUrl(link, currentDepth + 1)) {

                    /* Phaser ko batao — ek aur task aa raha hai
                     * Count badhao register() se
                     * Warna main thread wait karna band kar dega */
                    phaser.register();

                    /* Naya task submit karo thread pool mein —
                     * currentDepth + 1 pass karo — ek level aur andar gaye
                     * Koi bhi available thread ye task uthayega */
                    WebCrawler.submitTask(urlStore, urlFetcher,
                                         currentDepth + 1, maxDepth,
                                         databaseManager);
                }
            }

        } catch (Exception e) {
            /* Agar crawling fail hui — status failed mark karo DB mein
             * Ye important hai crash recovery ke liye —
             * Restart pe pata chalega kaun se URLs fail hue the
             * Unhe retry kar sakte hain
             *
             * url null check — agar queue empty thi toh
             * url hi nahi tha, DB update karne ki zarurat nahi */
            if (url != null) {
                databaseManager.updateUrlStatus(url, "failed");
            }
            System.out.println("Error crawling: " + url + " → " + e.getMessage());

        } finally {
            /* YE HAMESHA CHALEGA — exception aaye ya na aaye
             *
             * Phaser ko batao — mera kaam khatam hua
             * Count ghataao arriveAndDeregister() se
             *
             * Agar ye nahi kiya toh —
             * Main thread FOREVER wait karta rahega
             * Program kabhi band nahi hoga! 😱 */
            phaser.arriveAndDeregister();
        }
    }
}

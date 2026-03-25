package org.spidercore;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

/*
 * URLStore — Ye class URLs ka "manager" hai poore crawler mein.
 * 
 * 3 cheezein manage karta hai:
 * 1. ConcurrentHashMap → RAM cache (fast duplicate check)
 * 2. LinkedBlockingQueue → Work buffer (threads ka waiting line)
 * 3. DatabaseManager → Permanent storage (crash recovery)
 * 
 * Real world example:
 * Socho ek library hai —
 * ConcurrentHashMap = librarian ka dimag (recently dekhi books yaad hain)
 * Queue = books ki waiting list (kaunsi book next padhni hai)
 * Database = library ka register (permanent record sabka)
 */
public class URLStore {

    /* ConcurrentHashMap — RAM mein fast visited check ke liye
     * Normal HashMap use nahi kar sakte kyunki multiple threads
     * same time pe read/write karenge — data corrupt ho sakta tha
     * ConcurrentHashMap internally locking handle karta hai safely */
    private final ConcurrentHashMap<String, Boolean> visitedUrl = new ConcurrentHashMap<>();

    /* LinkedBlockingQueue — threads ka kaam ka "waiting line"
     * Threads yahan se URLs uthate hain crawl karne ke liye
     * Thread-safe hai aur producer-consumer pattern follow karta hai
     * Producer = jo URLs add karta hai
     * Consumer = jo URLs uthake crawl karta hai */
    private final BlockingQueue<String> urlQueue = new LinkedBlockingQueue<>();

    /* DatabaseManager ka reference — ye class DB se baat karegi
     * URLStore directly DB ko touch nahi karega
     * Ye Single Responsibility Principle hai — ek class ek kaam */
    private final DatabaseManager databaseManager;

    /* Constructor mein DatabaseManager inject karte hain
     * "Inject" matlab bahar se pass karo — URLStore khud nahi banayega
     * Ye pattern kehlaata hai Dependency Injection —
     * Isse testing easy hoti hai aur code loosely coupled rehta hai */
    public URLStore(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    /*
     * addUrl() — Naya URL add karna system mein
     *
     * 3 step process:
     * Step 1 → RAM cache check karo (fast) — duplicate toh nahi?
     * Step 2 → DB mein insert karo (permanent save)
     * Step 3 → Queue mein daalo (thread uthayega crawl karne)
     *
     * Depth parameter add kiya — pehle nahi tha
     * Kyun? DB mein depth store karni hai crash recovery ke liye
     * Aur ye bhi track karna hai ki URL kitne level andar mila tha
     *
     * Returns:
     * true  → URL naya tha, successfully add hua
     * false → URL already tha, duplicate skip kiya
     */
    public boolean addUrl(String url, int depth) {

        /* putIfAbsent() — atomic operation hai ye
         * Matlab ek hi step mein check bhi karta hai aur insert bhi
         * 2 threads same time pe same URL check karein toh bhi safe hai
         * null return hoga sirf tab jab URL pehli baar aa raha ho */
        if (visitedUrl.putIfAbsent(url, true) == null) {

            /* DB mein insert karo — permanent save
             * INSERT IGNORE use kiya DatabaseManager mein
             * Matlab agar DB mein already hai toh silently skip karega
             * Ye double safety net hai RAM cache ke upar */
            databaseManager.insertUrl(url, depth);

            /* Queue mein daalo — thread uthayega isse crawl karne ke liye
             * offer() use kiya kyunki queue unbounded hai
             * Matlab full nahi hogi — exception nahi aayega kabhi */
            urlQueue.offer(url);

            return true;
        }

        // URL already tha RAM cache mein — duplicate, skip karo
        return false;
    }

    /*
     * getNextUrl() — Queue se agla URL nikalna
     *
     * poll() — queue ka pehla item nikalta hai aur hata deta hai
     * Agar queue empty ho toh null return karta hai (wait nahi karta)
     *
     * CrawlerTask yahan se URL uthata hai crawl karne ke liye
     */
    public String getNextUrl() {
        return urlQueue.poll();
    }

    /*
     * isEmptyQueue() — Queue empty hai ya nahi check karna
     *
     * WebCrawler mein use hoga ye check karne ke liye ki
     * saara kaam khatam hua ya nahi
     */
    public boolean isEmptyQueue() {
        return urlQueue.isEmpty();
    }
}

# 🕷️ SpiderCore — Multithreaded Web Crawler

A production-inspired multithreaded web crawler built in Java that crawls websites concurrently, stores data persistently in MySQL, and supports crash recovery.

---

## 🚀 Features

- **Multithreaded Crawling** — Concurrent crawling using Java's `ExecutorService` with a configurable thread pool
- **BFS Traversal** — Crawls URLs level by level using Breadth First Search up to a configurable depth
- **MySQL Integration** — Persistent storage of URLs and crawled data, enabling crash recovery and resumable crawls
- **Thread-Safe Design** — Uses `ConcurrentHashMap` as RAM cache and `LinkedBlockingQueue` as work buffer
- **Phaser Synchronization** — Main thread waits for all crawling tasks to complete using Java's `Phaser`
- **Crawler Stats** — Real-time performance metrics including pages/sec, total crawled, and failed URLs
- **Crash Recovery** — On restart, pending URLs are preserved in DB and can be resumed
- **Robots.txt Compliance** — Respects `Disallow` rules for the `*` user-agent before crawling any URL, with per-domain caching to avoid repeated fetches
- **Retry with Exponential Backoff** — Failed page fetches are retried up to 3 times with increasing delay (500ms, 1s, 2s) before being marked as failed

---

## 🏗️ Architecture
```
WebCrawler (Controller)
    │
    ├── URLStore (URL Management)
    │       ├── ConcurrentHashMap (RAM Cache)
    │       ├── LinkedBlockingQueue (Work Buffer)
    │       └── DatabaseManager (Persistent Storage)
    │
    ├── URLFetcher (Network Layer)
    │       ├── RobotsTxtChecker (robots.txt rules, per-domain cache)
    │       ├── fetchWithRetry (exponential backoff, 3 attempts)
    │       └── CrawlResult (title + links)
    │
    ├── CrawlerTask (Worker - Runnable)
    │       ├── Fetch links via URLFetcher
    │       ├── Update DB status
    │       └── Submit new tasks
    │
    ├── DatabaseManager (MySQL Layer)
    │       ├── urls table
    │       └── crawled_data table
    │
    └── CrawlerStats (Metrics)
            └── AtomicInteger counters
```

---

## 🗄️ Database Schema
```sql
-- Tracks all URLs with their crawl status
CREATE TABLE urls (
    id         INT AUTO_INCREMENT PRIMARY KEY,
    url        VARCHAR(2048) NOT NULL,
    status     ENUM('pending','visited','failed') DEFAULT 'pending',
    depth      INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY unique_url (url(500))
);

-- Stores crawled page data
CREATE TABLE crawled_data (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    url_id      INT NOT NULL,
    title       VARCHAR(1024),
    links_found INT DEFAULT 0,
    crawled_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (url_id) REFERENCES urls(id)
);
```

---

## ⚙️ Tech Stack

| Technology | Usage |
|------------|-------|
| Java 21 | Core language |
| ExecutorService | Thread pool management |
| Phaser | Thread synchronization |
| MySQL | Persistent URL & data storage |
| JSoup | HTML parsing & link extraction |
| JDBC | Java-MySQL connectivity |
| Maven | Dependency management |

---

## 📁 Project Structure
```
src/main/java/org/spidercore/
    ├── Main.java            → Entry point
    ├── WebCrawler.java      → Controller, thread pool & phaser management
    ├── CrawlerTask.java     → Runnable task executed by each thread
    ├── URLStore.java        → URL management (cache + queue + DB)
    ├── URLFetcher.java      → Network calls, retry logic & HTML parsing via JSoup
    ├── RobotsTxtChecker.java → Fetches & parses robots.txt, caches disallowed paths per domain
    ├── CrawlResult.java     → Value object (title + links)
    ├── DatabaseManager.java → MySQL operations via JDBC
    └── CrawlerStats.java    → Performance metrics tracking
```

---

## 🛠️ Setup & Run

### Prerequisites
- Java 21+
- Maven
- MySQL 8.0+

### 1. Clone the repo
```bash
git clone https://github.com/thepratholic/SpiderCore.git
cd SpiderCore
```

### 2. Setup MySQL
```sql
CREATE DATABASE web_crawler;
USE web_crawler;

CREATE TABLE urls (
    id         INT AUTO_INCREMENT PRIMARY KEY,
    url        VARCHAR(2048) NOT NULL,
    status     ENUM('pending','visited','failed') DEFAULT 'pending',
    depth      INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY unique_url (url(500))
);

CREATE TABLE crawled_data (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    url_id      INT NOT NULL,
    title       VARCHAR(1024),
    links_found INT DEFAULT 0,
    crawled_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (url_id) REFERENCES urls(id)
);
```

### 3. Configure DB credentials
In `DatabaseManager.java`:
```java
private static final String URL = "jdbc:mysql://localhost:3306/web_crawler";
private static final String USER = "root";
private static final String PASSWORD = "your_password";
```

### 4. Build & Run
```bash
mvn compile
mvn exec:java -Dexec.mainClass="org.spidercore.Main"
```

### 5. Input
```
Enter your URL: https://wikipedia.org
Enter the Depth of the crawler: 2
Enter the number of workers: 4
```

---

## 📊 Sample Output
```
Database connected successfully!
pool-1-thread-1 crawling: https://wikipedia.org
pool-1-thread-2 crawling: https://en.wikipedia.org/
...

╔════════════════════════════════════╗
║         CRAWLER STATS REPORT       ║
╠════════════════════════════════════╣
║  Total URLs crawled  : 150         ║
║  Failed URLs         : 3           ║
║  Total time          : 45.0s       ║
║  Pages/sec           : 3.33        ║
╚════════════════════════════════════╝
```

---

## 💡 Key Design Decisions

**Why ConcurrentHashMap + Database?**
ConcurrentHashMap provides O(1) fast RAM-based duplicate checking during runtime. Database provides persistent storage for crash recovery — if the crawler stops mid-way, pending URLs survive and crawling can resume.

**Why Phaser over CountDownLatch?**
`CountDownLatch` requires knowing the total task count upfront. Since our crawler dynamically discovers new URLs during crawling, `Phaser` is ideal — it supports dynamic registration and deregistration of tasks at runtime.

**Why LinkedBlockingQueue?**
Acts as a thread-safe work buffer between URL discovery and crawling. Prevents all threads from hitting the database simultaneously — DB serves as source of truth, queue serves as controlled work distribution.

**Why cache robots.txt per domain?**
Fetching and parsing `robots.txt` on every single URL request would add unnecessary network overhead. `RobotsTxtChecker` caches disallowed paths per domain in a `ConcurrentHashMap` the first time a domain is seen, so all subsequent URLs on that domain are checked against the cached rules instead of re-fetching. It fails open (allows the crawl) if `robots.txt` is unreachable or malformed, so a broken robots file doesn't halt crawling.

**Why retry with exponential backoff?**
Transient network errors (timeouts, temporary server issues) shouldn't immediately mark a URL as failed. `URLFetcher` retries up to 3 times with increasing delays (500ms → 1s → 2s) before giving up, which improves crawl success rate on flaky connections without hammering the target server.

---

## 🔮 Future Improvements

- [ ] Rate limiting / politeness policy per domain
- [ ] REST API to monitor crawl progress in real-time
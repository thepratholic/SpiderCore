package org.spidercore;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.HashSet;
import java.util.Set;

public class URLFetcher {

    private final RobotsTxtChecker robotsTxtChecker = new RobotsTxtChecker();

    private static final int MAX_RETRIES = 3;
    private static final long BASE_BACKOFF_MS = 500;

    private Document fetchWithRetry(String url) {
        int attempt = 0;
        Exception lastException = null;

        while (attempt < MAX_RETRIES) {
            try {
                return Jsoup.connect(url).timeout(5000).get();
            } catch (Exception e) {
                lastException = e;
                attempt++;

                if (attempt < MAX_RETRIES) {
                    long backoffTime = BASE_BACKOFF_MS * (long) Math.pow(2, attempt - 1);
                    try {
                        Thread.sleep(backoffTime);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

    throw new RuntimeException("Failed to fetch URL after " + MAX_RETRIES + " attempts: " + url + " → " + lastException.getMessage());
}

    public CrawlResult fetchLinks(String url) {

        if (!robotsTxtChecker.isAllowed(url)) {
            throw new RuntimeException("Blocked by robots.txt: " + url);
        }
        
        Set<String> links = new HashSet<>();

        Document document = fetchWithRetry(url);

        try {
            document = Jsoup.connect(url).timeout(5000).get();
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch URL: " + url + " → " + e.getMessage());
        }

        String title = document.title().isEmpty() ? "No Title" : document.title();

        Elements anchorTags = document.select("a[href]");

        for (Element link : anchorTags) {
 
            String extractedUrl = link.absUrl("href");

            if (!extractedUrl.isEmpty()) {
                links.add(extractedUrl);
            }
        }

        return new CrawlResult(title, links);
    }
}
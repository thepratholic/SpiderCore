package org.spidercore;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.HashSet;
import java.util.Set;

public class URLFetcher {

    private final RobotsTxtChecker robotsTxtChecker = new RobotsTxtChecker();

    public CrawlResult fetchLinks(String url) {

        if (!robotsTxtChecker.isAllowed(url)) {
            throw new RuntimeException("Blocked by robots.txt: " + url);
        }
        
        Set<String> links = new HashSet<>();

        Document document = null;

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
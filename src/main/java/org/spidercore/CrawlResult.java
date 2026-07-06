package org.spidercore;

public class CrawlResult {
    private final String title;
    private final java.util.Set<String> links;

    public CrawlResult(String title, java.util.Set<String> links) {
        this.title = title;
        this.links = links;
    }

    public String getTitle() {
        return title;
    }

    public java.util.Set<String> getLinks() {
        return links;
    }
}
package org.spidercore;

/*
 * CrawlResult — Ek simple wrapper class hai ye
 *
 * Problem kya thi? —
 * fetchLinks() sirf Set<String> return karta tha — sirf links
 * Title return karna ho toh? Java mein ek method ek hi cheez return kar sakta hai
 *
 * Solution — dono cheezein ek object mein wrap karo
 * Jaise delivery mein ek box mein sab kuch pack karo
 *
 * Ye pattern kehlaata hai — "Value Object"
 * Sirf data hold karta hai, koi logic nahi
 */
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
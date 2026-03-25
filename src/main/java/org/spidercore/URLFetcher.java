package org.spidercore;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.HashSet;
import java.util.Set;

public class URLFetcher {

    /* This method basically returns the set of urls which are present inside the url provided
        in the function definition.
     */
    public Set<String> fetchLinks(String url) {
        Set<String> links = new HashSet<>();

        // when we fetch any data from any website, it is in the form of document.
        // so this document will later hold the HTML of webpage.
        Document document = null;

        // we are getting the HTML , but it is risky as we are doing the network call.
        // So that is try/catch is used.
        // Errors can be : URL might not exist, Server may be down, Timeout, 404 errors
        try {
            // timeout() says to Jsoup that wait for max 5seconds for the server to respond.
            // if it takes longer, than throw an error.
            document = Jsoup.connect(url).timeout(5000).get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        System.out.println(document.text()); // It prints all the visible text from the webpage.

        // It grabs all the <a> tags (links) from the webpage that actually have an href attribute.
        // So basically → all clickable links on that page.
        // So JSoup returns a list of all valid hyperlink elements.
        Elements anchorTags = document.select("a[href]");

        // Element is the child class of Elements, and each link is the <a> element.
        for (Element link : anchorTags) {
            // absUrl converts relative links to absolute links

            // Example:

            //If page URL = https://example.com/about
            //And the link is <a href="/contact">Contact</a>
            //Then: link.absUrl("href")
            //becomes: https://example.com/contact

            //JSoup automatically fills in the base URL
            // Makes crawling way easier
            String extractedUrl = link.absUrl("href");

            if(!extractedUrl.isEmpty()) {
                links.add(extractedUrl);
            }

            /* We are doing this coz, Because our crawler needs actual URLs to push into:
                Your BlockingQueue, ConcurrentHashMap visited set
             */
        }

        return links;
    }
}

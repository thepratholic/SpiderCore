package org.spidercore;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.HashSet;
import java.util.Set;

/*
 * URLFetcher — Ye class actual network call karta hai
 * aur webpage ka data fetch karta hai.
 *
 * Kya karta hai:
 * 1. URL pe connect karta hai (network call)
 * 2. HTML document fetch karta hai
 * 3. Title extract karta hai
 * 4. Saare links extract karta hai
 * 5. CrawlResult mein wrap karke return karta hai
 *
 * Real world example:
 * Ye ek "scout" hai —
 * Ek jagah jaata hai, information collect karta hai
 * aur report (CrawlResult) lekar aata hai
 */
public class URLFetcher {

    /*
     * fetchLinks() — Ye method kisi bhi URL pe jaake
     * uska title aur saare links fetch karta hai
     *
     * Pehle sirf links return hote the — Set<String>
     * Ab CrawlResult return hota hai — title + links dono
     *
     * Typo bhi fix kiya — pehle "fecthLinks" tha 😅
     */
    public CrawlResult fetchLinks(String url) {
        Set<String> links = new HashSet<>();

        /* Document — JSoup ka object jo poora HTML hold karta hai
         * Jaise browser mein ek webpage load hoti hai
         * waise ye object mein HTML load hoti hai */
        Document document = null;

        try {
            /* Jsoup.connect() — actual network call
             * timeout(5000) — 5 seconds se zyada wait mat karo
             * Agar server slow hai ya down hai toh exception aayega
             *
             * Errors jo aa sakte hain:
             * - URL exist nahi karta (404)
             * - Server down hai
             * - Network timeout
             * - SSL certificate invalid */
            document = Jsoup.connect(url).timeout(5000).get();
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch URL: " + url + " → " + e.getMessage());
        }

        /* Title extract karo —
         * document.title() — webpage ka <title> tag content
         * Jaise browser tab pe jo naam dikhta hai
         *
         * Agar title empty ho toh "No Title" rakho —
         * DB mein null save nahi karni */
        String title = document.title().isEmpty() ? "No Title" : document.title();

        /* document.select("a[href]") —
         * Saare <a> tags jo href attribute rakhte hain
         * Ye saare clickable links hain webpage pe
         *
         * Example HTML: <a href="/about">About Us</a>
         * Ye select ho jaayega */
        Elements anchorTags = document.select("a[href]");

        for (Element link : anchorTags) {
            /* absUrl("href") — relative URL ko absolute banata hai
             *
             * Example:
             * Page URL = https://wikipedia.org/home
             * Link href = /about
             * absUrl result = https://wikipedia.org/about
             *
             * Ye zaroori hai kyunki crawler ko
             * complete URL chahiye crawl karne ke liye */
            String extractedUrl = link.absUrl("href");

            if (!extractedUrl.isEmpty()) {
                links.add(extractedUrl);
            }
        }

        /* CrawlResult mein dono cheezein wrap karke return karo
         * title — DB mein save hoga crawled_data mein
         * links — in pe naye CrawlerTasks banenge */
        return new CrawlResult(title, links);
    }
}
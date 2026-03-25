package org.spidercore;

/*
 * Main — Ye project ka actual entry point hai
 *
 * Abhi WebCrawler.java mein main() method hai —
 * hum wahan se hi crawler start karenge
 *
 * Ye class simply WebCrawler ka main() call karegi
 * Separation of concerns —
 * Main = sirf start karo
 * WebCrawler = actual logic
 */
public class Main {
    public static void main(String[] args) {
        WebCrawler.main(args);
    }
}
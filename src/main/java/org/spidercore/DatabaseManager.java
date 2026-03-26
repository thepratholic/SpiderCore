package org.spidercore;


import java.sql.*;

/*
 * DatabaseManager — Ye class poore project mein DB ki
 * "gatekeeper" hai. Sirf yahi class MySQL se baat karegi.
 * Baaki saari classes (URLStore, CrawlerTask) isse use
 * karengi — directly DB ko touch nahi karengi.
 *
 * Ye pattern kehlaata hai — "Single Responsibility Principle"
 * Ek class, ek kaam.
 */
public class DatabaseManager {

    // Ye teen cheezein MySQL se connection banane ke liye zaroori hain
    // jdbc:mysql → protocol (jaise https:// hota hai browser mein)
    // localhost   → DB hamare hi machine pe hai
    // 3306        → MySQL ka default port
    // web_crawler → humara database name
    private static final String URL = "jdbc:mysql://localhost:3306/web_crawler";
    private static final String USER = "root";
    private static final String PASSWORD = "Apple@2019()"; // apna password daalna

    // Connection object — ye actual "pipe" hai Java aur MySQL ke beech
    // Jaise phone call pe line open hoti hai, ye DB ke saath line open rakhta hai
    private Connection connection;

    /*
     * Constructor mein hi connection banana — kyunki jab bhi
     * DatabaseManager ka object banega, DB se connected hona zaroori hai
     * tabhi toh koi kaam kar payega
     */
    public DatabaseManager() {
        try {
            // DriverManager.getConnection() — ye actual connection banata hai
            // Jaise dial karo aur call connect ho jaye
            this.connection = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("Database connected successfully!");
        } catch (SQLException e) {
            // Agar DB connect na ho — poora crawler kaam nahi karega
            // Isiliye RuntimeException throw karte hain — critical failure hai ye
            throw new RuntimeException("Database connection failed: " + e.getMessage());
        }
    }

    /*
     * insertUrl() — Naya URL DB mein daalna
     *
     * "INSERT IGNORE" kyun? —
     * Agar same URL dobara insert karne ki koshish ho
     * (2 threads same time pe) toh IGNORE silently skip kar dega
     * Error nahi aayega — ye UNIQUE constraint ka smart use hai
     *
     * Ye same kaam karta tha pehle:
     * ConcurrentHashMap.putIfAbsent(url, true)
     */
    public void insertUrl(String url, int depth) {
        String query = "INSERT IGNORE INTO urls (url, status, depth) VALUES (?, 'pending', ?)";

        try {
            // PreparedStatement — query mein directly string mat daalo
            // Kyunki agar URL mein special characters hain toh query toot sakti hai
            // PreparedStatement safely handle karta hai — SQL Injection bhi rokta hai
            PreparedStatement stmt = connection.prepareStatement(query);
            stmt.setString(1, url);   // pehla '?' replace hoga url se
            stmt.setInt(2, depth);    // doosra '?' replace hoga depth se
            stmt.executeUpdate();     // query actually run karo DB pe
        } catch (SQLException e) {
            System.out.println("Error inserting URL: " + e.getMessage());
        }
    }

    /*
     * updateUrlStatus() — URL ka status change karna
     * pending → visited (jab crawl ho jaye)
     * pending → failed  (jab error aaye)
     *
     * Ye same tha pehle:
     * ConcurrentHashMap mein URL tha matlab visited — ab explicitly status store hoga
     */
    public void updateUrlStatus(String url, String status) {
        String query = "UPDATE urls SET status = ? WHERE url = ?";

        try {
            PreparedStatement stmt = connection.prepareStatement(query);
            stmt.setString(1, status);
            stmt.setString(2, url);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Error updating URL status: " + e.getMessage());
        }
    }

    /*
     * isVisited() — Check karna ki URL pehle crawl ho chuka hai ya nahi
     *
     * Ye RAM cache (ConcurrentHashMap) ka backup hai —
     * Restart ke baad cache empty hoga, tab DB se check karenge
     * Normal flow mein ConcurrentHashMap fast check karega
     */
    public boolean isVisited(String url) {
        String query = "SELECT status FROM urls WHERE url = ? AND status = 'visited'";

        try {
            PreparedStatement stmt = connection.prepareStatement(query);
            stmt.setString(1, url);

            // ResultSet — DB ka answer aata hai isme
            // Jaise ek table return hoti hai query ka result
            ResultSet rs = stmt.executeQuery();
            return rs.next(); // agar row mili toh visited hai
        } catch (SQLException e) {
            System.out.println("Error checking URL: " + e.getMessage());
            return false;
        }
    }

    /*
     * saveCrawledData() — Page ka data store karna
     *
     * Abhi tera code sirf console pe print karta tha:
     * System.out.println(document.text()) — ye data gayab ho jaata tha
     *
     * Ab ye permanently DB mein save hoga — restart ke baad bhi rahega
     */
    public void saveCrawledData(int urlId, String title, int linksFound) {
        String query = "INSERT INTO crawled_data (url_id, title, links_found) VALUES (?, ?, ?)";

        try {
            PreparedStatement stmt = connection.prepareStatement(query);
            stmt.setInt(1, urlId);
            stmt.setString(2, title);
            stmt.setInt(3, linksFound);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Error saving crawled data: " + e.getMessage());
        }
    }

    /*
     * getUrlId() — URL ka DB id lana
     *
     * Kyun chahiye ye? —
     * crawled_data table mein url_id store hota hai (FOREIGN KEY)
     * Pehle url ka id dhundna padega, phir crawled_data mein save hoga
     */
    public int getUrlId(String url) {
        String query = "SELECT id FROM urls WHERE url = ?";

        try {
            PreparedStatement stmt = connection.prepareStatement(query);
            stmt.setString(1, url);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getInt("id");
            }
        } catch (SQLException e) {
            System.out.println("Error getting URL id: " + e.getMessage());
        }
        return -1; // -1 matlab URL mila nahi DB mein
    }

    /*
     * closeConnection() — DB connection band karna
     *
     * Ye bahut zaroori hai — open connection resources consume karta hai
     * Jaise phone call khatam hone pe line disconnect karo
     * Crawler band hone pe ye call karenge
     */
    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("Database connection closed.");
            }
        } catch (SQLException e) {
            System.out.println("Error closing connection: " + e.getMessage());
        }
    }
}
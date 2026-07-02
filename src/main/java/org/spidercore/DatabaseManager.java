package org.spidercore;
import java.sql.*;

public class DatabaseManager {

    private static final String URL = "jdbc:mysql://localhost:3306/web_crawler";
    private static final String USER = "root";
    private static final String PASSWORD = "Apple@2019()"; // apna password daalna

    private Connection connection;

    public DatabaseManager() {
        try {
            this.connection = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("Database connected successfully!");
        } catch (SQLException e) {
            throw new RuntimeException("Database connection failed: " + e.getMessage());
        }
    }

    public void insertUrl(String url, int depth) {
        String query = "INSERT IGNORE INTO urls (url, status, depth) VALUES (?, 'pending', ?)";

        try {
            PreparedStatement stmt = connection.prepareStatement(query);
            stmt.setString(1, url);   // pehla '?' replace hoga url se
            stmt.setInt(2, depth);    // doosra '?' replace hoga depth se
            stmt.executeUpdate();     // query actually run karo DB pe
        } catch (SQLException e) {
            System.out.println("Error inserting URL: " + e.getMessage());
        }
    }

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
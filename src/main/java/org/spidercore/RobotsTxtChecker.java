package org.spidercore;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

public class RobotsTxtChecker {

    private final ConcurrentHashMap<String, List<String>> disallowedPathsCache = new ConcurrentHashMap<>();

    public boolean isAllowed(String url) {
        try {
            URI parsedUri = URI.create(url);
            String domain = parsedUri.getScheme() + "://" + parsedUri.getHost();
            String path = (parsedUri.getPath() == null || parsedUri.getPath().isEmpty()) ? "/" : parsedUri.getPath();

            List<String> disallowedPaths = disallowedPathsCache.computeIfAbsent(domain, this::fetchDisallowedPaths);

            for (String disallowedPath : disallowedPaths) {
                if (path.startsWith(disallowedPath)) {
                    return false;
                }
            }
            return true;

        } catch (Exception e) {
            return true; // fail-open
        }
    }

    private List<String> fetchDisallowedPaths(String domain) {
        List<String> disallowedPaths = new ArrayList<>();

        try {
            URL robotsUrl = URI.create(domain + "/robots.txt").toURL();
            HttpURLConnection connection = (HttpURLConnection) robotsUrl.openConnection();
            connection.setConnectTimeout(3000);
            connection.setReadTimeout(3000);

            if (connection.getResponseCode() != 200) {
                return disallowedPaths;
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String line;
            boolean isRelevantUserAgent = false;

            while ((line = reader.readLine()) != null) {
                line = line.trim();

                if (line.toLowerCase().startsWith("user-agent:")) {
                    String agent = line.substring("user-agent:".length()).trim();
                    isRelevantUserAgent = agent.equals("*");
                } else if (isRelevantUserAgent && line.toLowerCase().startsWith("disallow:")) {
                    String disallowedPath = line.substring("disallow:".length()).trim();
                    if (!disallowedPath.isEmpty()) {
                        disallowedPaths.add(disallowedPath);
                    }
                }
            }
            reader.close();

        } catch (Exception e) {
            // fail-open
        }

        return disallowedPaths;
    }
}
package com.bot;

import java.sql.*;
import java.util.*;

public class Database {

    private static String DB_URL;
    private static String DB_USER;
    private static String DB_PASSWORD;

    static {
        String dbUrl = System.getenv("DATABASE_URL");
        
        if (dbUrl == null || dbUrl.isEmpty()) {
            dbUrl = "postgres://postgres:postgres@localhost:5432/postgres";
        }
        
        try {
            if (dbUrl.startsWith("postgres://")) {
                dbUrl = dbUrl.substring("postgres://".length());
                
                String[] parts = dbUrl.split("/");
                String hostPart = parts[0];
                String dbName = parts.length > 1 ? parts[1] : "postgres";
                
                String[] hostPort = hostPart.split(":");
                String host = hostPort[0];
                String port = hostPort.length > 1 ? hostPort[1] : "5432";
                
                String[] userPass = hostPart.split("@")[0].split(":");
                DB_USER = userPass[0];
                DB_PASSWORD = userPass.length > 1 ? userPass[1] : "";
                
                dbUrl = "jdbc:postgresql://" + host + ":" + port + "/" + dbName;
            }
            
            String manualUser = System.getenv("DB_USER");
            String manualPass = System.getenv("DB_PASSWORD");
            
            if (manualUser != null && !manualUser.isEmpty()) {
                DB_USER = manualUser;
            }
            if (manualPass != null && !manualPass.isEmpty()) {
                DB_PASSWORD = manualPass;
            }
            
            DB_URL = dbUrl;
            System.out.println("🔗 Connecting to: " + DB_URL + " as " + DB_USER);
            
            try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
                Statement stmt = conn.createStatement();
                stmt.execute("""
                    CREATE TABLE IF NOT EXISTS documents (
                        user_id TEXT,
                        tag TEXT,
                        file_id TEXT
                    )
                """);
                System.out.println("✅ DATABASE CONNECTED");
            }
        } catch (Exception e) {
            System.out.println("❌ DATABASE CONNECTION FAILED: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void save(String userId, String tag, List<String> fileIds) throws Exception {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {

            for (String fileId : fileIds) {
                PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO documents (user_id, tag, file_id) VALUES (?, ?, ?)"
                );

                ps.setString(1, userId);
                ps.setString(2, tag.toLowerCase());
                ps.setString(3, fileId);

                ps.executeUpdate();
            }
        }
    }

    public static void replace(String userId, String tag, List<String> fileIds) throws Exception {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {

            PreparedStatement del = conn.prepareStatement(
                    "DELETE FROM documents WHERE user_id=? AND tag=?"
            );
            del.setString(1, userId);
            del.setString(2, tag.toLowerCase());
            del.executeUpdate();

            save(userId, tag, fileIds);
        }
    }

    public static boolean exists(String userId, String tag) throws Exception {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {

            PreparedStatement ps = conn.prepareStatement(
                    "SELECT 1 FROM documents WHERE user_id=? AND tag=? LIMIT 1"
            );

            ps.setString(1, userId);
            ps.setString(2, tag.toLowerCase());

            ResultSet rs = ps.executeQuery();
            return rs.next();
        }
    }

    public static List<String> search(String userId, String input) throws Exception {
        List<String> result = new ArrayList<>();

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {

            PreparedStatement ps = conn.prepareStatement(
                    "SELECT file_id FROM documents WHERE user_id=? AND tag LIKE ?"
            );

            ps.setString(1, userId);
            ps.setString(2, "%" + input.toLowerCase() + "%");

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                result.add(rs.getString("file_id"));
            }
        }

        return result;
    }

    public static Set<String> getTags(String userId) throws Exception {
        Set<String> tags = new HashSet<>();

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {

            PreparedStatement ps = conn.prepareStatement(
                    "SELECT DISTINCT tag FROM documents WHERE user_id=?"
            );

            ps.setString(1, userId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                tags.add(rs.getString("tag"));
            }
        }

        return tags;
    }
}
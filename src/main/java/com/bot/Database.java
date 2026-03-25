package com.bot;

import java.sql.*;
import java.util.*;
import java.net.URI;

public class Database {

    private static String DB_URL;
    private static String DB_USER;
    private static String DB_PASSWORD;

    static {
        DB_URL = System.getenv("DATABASE_URL");
        if (DB_URL == null || DB_URL.isEmpty()) {
            DB_URL = System.getProperty("DATABASE_URL", "postgres://postgres:postgres@localhost:5432/postgres");
        }
        
        String user = System.getenv("DB_USER");
        String password = System.getenv("DB_PASSWORD");
        
        if (user == null || password == null) {
            try {
                URI uri = new URI(DB_URL);
                String[] userInfo = uri.getUserInfo().split(":");
                DB_USER = userInfo[0];
                DB_PASSWORD = userInfo.length > 1 ? userInfo[1] : "";
            } catch (Exception e) {
                DB_USER = "postgres";
                DB_PASSWORD = "postgres";
            }
        } else {
            DB_USER = user;
            DB_PASSWORD = password;
        }

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

        } catch (Exception e) {
            System.out.println("❌ DATABASE CONNECTION FAILED: " + e.getMessage());
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
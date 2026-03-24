package com.bot;

import java.sql.*;
import java.util.*;

public class Database {

    private static final String DB_URL = "jdbc:sqlite:documents.db";

    public static void init() {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {
            Statement stmt = conn.createStatement();

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS documents (
                    user_id TEXT,
                    tag TEXT,
                    file_id TEXT
                )
            """);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void saveFile(String userId, String tag, String fileId) throws Exception {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {

            PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO documents (user_id, tag, file_id) VALUES (?, ?, ?)"
            );

            ps.setString(1, userId);
            ps.setString(2, tag.toLowerCase());
            ps.setString(3, fileId);

            ps.executeUpdate();
        }
    }

    public static List<String> getFiles(String userId, String tag) throws Exception {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {

            PreparedStatement ps = conn.prepareStatement(
                    "SELECT file_id FROM documents WHERE user_id=? AND tag LIKE ?"
            );

            ps.setString(1, userId);
            ps.setString(2, "%" + tag.toLowerCase() + "%");

            ResultSet rs = ps.executeQuery();

            List<String> list = new ArrayList<>();
            while (rs.next()) {
                list.add(rs.getString("file_id"));
            }

            return list;
        }
    }

    public static List<String> getTags(String userId) throws Exception {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {

            PreparedStatement ps = conn.prepareStatement(
                    "SELECT DISTINCT tag FROM documents WHERE user_id=?"
            );

            ps.setString(1, userId);
            ResultSet rs = ps.executeQuery();

            List<String> list = new ArrayList<>();
            while (rs.next()) {
                list.add(rs.getString("tag"));
            }

            return list;
        }
    }

    public static void deleteTag(String userId, String tag) throws Exception {
        try (Connection conn = DriverManager.getConnection(DB_URL)) {

            PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM documents WHERE user_id=? AND tag=?"
            );

            ps.setString(1, userId);
            ps.setString(2, tag.toLowerCase());

            ps.executeUpdate();
        }
    }
}
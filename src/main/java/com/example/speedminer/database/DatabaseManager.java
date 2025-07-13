package com.example.speedminer.database;

import com.example.speedminer.SpeedMiner;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DatabaseManager {
    private final SpeedMiner plugin;
    private Connection connection;

    public record PlayerStat(String name, int points) {}

    public DatabaseManager(SpeedMiner plugin) {
        this.plugin = plugin;
        setup();
    }

    private void setup() {
        try {
            String url = plugin.getConfig().getString("database.url", "jdbc:sqlite:plugins/SpeedMiner/speedminer.db");
            String user = plugin.getConfig().getString("database.user", "");
            String pass = plugin.getConfig().getString("database.password", "");
            connection = DriverManager.getConnection(url, user, pass);
            try (Statement stmt = connection.createStatement()) {
                stmt.executeUpdate(
                        "CREATE TABLE IF NOT EXISTS speedminer_stats(" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "uuid TEXT NOT NULL," +
                        "name TEXT NOT NULL," +
                        "points INTEGER NOT NULL," +
                        "last_played TIMESTAMP DEFAULT CURRENT_TIMESTAMP");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void saveStats(UUID uuid, String name, int points) {
        if (connection == null) return;
        try (PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO speedminer_stats(uuid, name, points) VALUES(?,?,?)")) {
            ps.setString(1, uuid.toString());
            ps.setString(2, name);
            ps.setInt(3, points);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public int loadStats(String name) {
        if (connection == null) return 0;
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT SUM(points) FROM speedminer_stats WHERE name=?")) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public List<PlayerStat> getTopPlayers(int limit) {
        List<PlayerStat> list = new ArrayList<>();
        if (connection == null) return list;
        String sql = "SELECT name, SUM(points) as total FROM speedminer_stats GROUP BY name ORDER BY total DESC LIMIT ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new PlayerStat(rs.getString("name"), rs.getInt("total")));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public void close() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}

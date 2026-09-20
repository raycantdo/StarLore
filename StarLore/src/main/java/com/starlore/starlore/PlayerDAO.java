package com.starlore.starlore;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class PlayerDAO {

    public Player checkOrCreatePlayer(String username) throws Exception {
        Connection conn = DatabaseConnection.getConnection();

        // Ensure database table and columns support permanent enlightenment
        initDatabaseSchema(conn);

        String checkSQL = "SELECT * FROM players WHERE username = ?";
        PreparedStatement checkStmt = conn.prepareStatement(checkSQL);
        checkStmt.setString(1, username);
        ResultSet rs = checkStmt.executeQuery();

        if (rs.next()) {
            // Returning player
            Player player = new Player(username, false);

            // Safely attempt to load extended stats. If the column doesn't exist yet, it ignores the error.
            try { player.setRankTitle(rs.getString("rank_title")); } catch (Exception ignored) {}
            try { player.setConstellationsMastered(rs.getInt("constellations_mastered")); } catch (Exception ignored) {}
            try { player.setTotalHoursPlayed(rs.getDouble("total_hours_played")); } catch (Exception ignored) {}
            try { player.setDuelWins(rs.getInt("duel_wins")); } catch (Exception ignored) {}
            try { player.setHighestArcadeScore(rs.getInt("highest_arcade_score")); } catch (Exception ignored) {}
            try { player.setStarsCaught(rs.getInt("stars_caught")); } catch (Exception ignored) {}
            try { player.setBestCombo(rs.getInt("best_combo")); } catch (Exception ignored) {}
            try { player.setTotalStarDust(rs.getInt("total_star_dust")); } catch (Exception ignored) {}

            // Load enlightened constellations string (comma-separated: e.g. "ARIES,TAURUS,ORION")
            try {
                String enlightenedStr = rs.getString("enlightened_constellations");
                if (enlightenedStr != null && !enlightenedStr.trim().isEmpty()) {
                    String[] names = enlightenedStr.split(",");
                    java.util.Set<String> set = new java.util.HashSet<>();
                    for (String n : names) {
                        if (!n.trim().isEmpty()) set.add(n.trim().toUpperCase());
                    }
                    player.setEnlightenedConstellations(set);
                }
            } catch (Exception ignored) {}

            // Also load from separate player_enlightened table if present
            loadEnlightenedFromTable(conn, username, player);

            // Safely attempt to update last login
            try {
                String updateSQL = "UPDATE players SET last_login = NOW() WHERE username = ?";
                PreparedStatement updateStmt = conn.prepareStatement(updateSQL);
                updateStmt.setString(1, username);
                updateStmt.executeUpdate();
            } catch (Exception ignored) {}

            conn.close();
            return player;
        } else {
            // New player — Safely attempt to insert with or without last_login column
            try {
                String insertSQL = "INSERT INTO players (username, last_login) VALUES (?, NOW())";
                PreparedStatement insertStmt = conn.prepareStatement(insertSQL);
                insertStmt.setString(1, username);
                insertStmt.executeUpdate();
            } catch (Exception e) {
                // Ultimate fallback if last_login column is missing
                String fallbackSQL = "INSERT INTO players (username) VALUES (?)";
                PreparedStatement fallbackStmt = conn.prepareStatement(fallbackSQL);
                fallbackStmt.setString(1, username);
                fallbackStmt.executeUpdate();
            }
            conn.close();
            return new Player(username, true);
        }
    }

    /**
     * Permanently saves an enlightened constellation for a player nickname in the database.
     */
    public void saveEnlightenedConstellation(String username, String constellationName, int totalStarDust) {
        if (username == null || constellationName == null) return;
        new Thread(() -> {
            try (Connection conn = DatabaseConnection.getConnection()) {
                initDatabaseSchema(conn);

                // 1. Save to player_enlightened table
                try {
                    String sql = "INSERT IGNORE INTO player_enlightened (username, constellation_name, enlightened_at) VALUES (?, ?, NOW())";
                    try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                        stmt.setString(1, username);
                        stmt.setString(2, constellationName.toUpperCase());
                        stmt.executeUpdate();
                    }
                } catch (Exception ignored) {}

                // 2. Also append to enlightened_constellations column in players table
                try {
                    String query = "SELECT enlightened_constellations, constellations_mastered FROM players WHERE username = ?";
                    String currentList = "";
                    int mastered = 0;
                    try (PreparedStatement qStmt = conn.prepareStatement(query)) {
                        qStmt.setString(1, username);
                        ResultSet rs = qStmt.executeQuery();
                        if (rs.next()) {
                            currentList = rs.getString("enlightened_constellations");
                            mastered = rs.getInt("constellations_mastered");
                        }
                    }

                    java.util.Set<String> set = new java.util.HashSet<>();
                    if (currentList != null && !currentList.trim().isEmpty()) {
                        for (String s : currentList.split(",")) {
                            if (!s.trim().isEmpty()) set.add(s.trim().toUpperCase());
                        }
                    }
                    set.add(constellationName.toUpperCase());
                    String updatedList = String.join(",", set);

                    String updateSQL =
                            "UPDATE players SET enlightened_constellations = ?, " +
                                    "constellations_mastered = ? " +
                                    "WHERE username = ?";
                    try (PreparedStatement uStmt = conn.prepareStatement(updateSQL)) {
                        uStmt.setString(1, updatedList);
                        uStmt.setInt(2, set.size());
                        uStmt.setString(3, username);
                        uStmt.executeUpdate();
                    }
                } catch (Exception ignored) {}

            } catch (Exception e) {
                System.err.println("[PlayerDAO] Error saving enlightened constellation: " + e.getMessage());
            }
        }).start();
    }

    private void loadEnlightenedFromTable(Connection conn, String username, Player player) {
        try {
            String sql = "SELECT constellation_name FROM player_enlightened WHERE username = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, username);
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    String cName = rs.getString("constellation_name");
                    if (cName != null) {
                        player.enlightenConstellation(cName);
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    private void initDatabaseSchema(Connection conn) {
        // Ensure table player_enlightened exists
        try {
            String createTableSQL = "CREATE TABLE IF NOT EXISTS player_enlightened (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "username VARCHAR(100) NOT NULL, " +
                    "constellation_name VARCHAR(100) NOT NULL, " +
                    "enlightened_at DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                    "UNIQUE KEY unique_user_const (username, constellation_name)" +
                    ")";
            try (PreparedStatement stmt = conn.prepareStatement(createTableSQL)) {
                stmt.executeUpdate();
            }
        } catch (Exception ignored) {}

        // Ensure players table has enlightened_constellations column
        try {
            String alterSQL = "ALTER TABLE players ADD COLUMN enlightened_constellations TEXT";
            // Ensure players table has total_star_dust column
            String starDustAlterSQL =
                    "ALTER TABLE players ADD COLUMN total_star_dust INT NOT NULL DEFAULT 0";

            try (PreparedStatement stmt = conn.prepareStatement(starDustAlterSQL)) {
                stmt.executeUpdate();
            } catch (Exception ignored) {}
            try (PreparedStatement stmt = conn.prepareStatement(alterSQL)) {
                stmt.executeUpdate();
            }
        } catch (Exception ignored) {}
    }
    public void updateStarDust(String username, int totalStarDust) {
        if (username == null) return;

        String sql = "UPDATE players SET total_star_dust = ? WHERE username = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, totalStarDust);
            stmt.setString(2, username);

            stmt.executeUpdate();

        } catch (Exception e) {
            System.err.println("[PlayerDAO] Error updating StarDust: " + e.getMessage());
        }
    }
    public java.util.List<String[]> getLeaderboard() {
        java.util.List<String[]> leaderboard = new java.util.ArrayList<>();

        String sql = "SELECT username, total_star_dust " +
                "FROM players " +
                "ORDER BY total_star_dust DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                String username = rs.getString("username");
                int starDust = rs.getInt("total_star_dust");

                leaderboard.add(new String[]{
                        username,
                        String.valueOf(starDust)
                });
            }

        } catch (Exception e) {
            System.err.println("[PlayerDAO] Error loading leaderboard: " + e.getMessage());
        }

        return leaderboard;
    }
}
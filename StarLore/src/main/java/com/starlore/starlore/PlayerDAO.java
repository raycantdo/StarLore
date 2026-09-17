package com.starlore.starlore;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class PlayerDAO {

    public Player checkOrCreatePlayer(String username) throws Exception {
        Connection conn = DatabaseConnection.getConnection();

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
}
package com.starlore.starlore;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class PlayerDAO {

    // Check if player exists, return Player object
    public Player checkOrCreatePlayer(String username) throws Exception {
        Connection conn = DatabaseConnection.getConnection();

        // Check if exists
        String checkSQL = "SELECT * FROM players WHERE username = ?";
        PreparedStatement checkStmt = conn.prepareStatement(checkSQL);
        checkStmt.setString(1, username);
        ResultSet rs = checkStmt.executeQuery();

        if (rs.next()) {
            // Returning player
            Player player = new Player(username, false);
            player.setRankTitle(rs.getString("rank_title"));
            player.setConstellationsMastered(rs.getInt("constellations_mastered"));
            player.setTotalHoursPlayed(rs.getDouble("total_hours_played"));
            player.setDuelWins(rs.getInt("duel_wins"));
            player.setHighestArcadeScore(rs.getInt("highest_arcade_score"));
            player.setStarsCaught(rs.getInt("stars_caught"));
            player.setBestCombo(rs.getInt("best_combo"));
            player.setTotalStarDust(rs.getInt("total_star_dust"));
            // Update last login
            String updateSQL = "UPDATE players SET last_login = NOW() WHERE username = ?";
            PreparedStatement updateStmt = conn.prepareStatement(updateSQL);
            updateStmt.setString(1, username);
            updateStmt.executeUpdate();

            conn.close();
            return player;
        } else {
            // New player — insert
            String insertSQL = "INSERT INTO players (username, last_login) VALUES (?, NOW())";
            PreparedStatement insertStmt = conn.prepareStatement(insertSQL);
            insertStmt.setString(1, username);
            insertStmt.executeUpdate();
            conn.close();
            return new Player(username, true);
        }
    }
}
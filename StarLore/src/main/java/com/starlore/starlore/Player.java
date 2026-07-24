package com.starlore.starlore;

public class Player {
    private int id;
    private String username;
    private String rankTitle;
    private int constellationsMastered;
    private double totalHoursPlayed;
    private int duelWins;
    private int highestArcadeScore;
    private boolean isNewPlayer;
    private int starsCaught;
    private int bestCombo;
    private int totalStarDust;
    public Player(String username, boolean isNewPlayer) {
        this.username = username;
        this.isNewPlayer = isNewPlayer;
    }

    // Getters and Setters
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getRankTitle() { return rankTitle; }
    public void setRankTitle(String rankTitle) { this.rankTitle = rankTitle; }
    public int getConstellationsMastered() { return constellationsMastered; }
    public void setConstellationsMastered(int constellationsMastered) { this.constellationsMastered = constellationsMastered; }
    public double getTotalHoursPlayed() { return totalHoursPlayed; }
    public void setTotalHoursPlayed(double totalHoursPlayed) { this.totalHoursPlayed = totalHoursPlayed; }
    public int getDuelWins() { return duelWins; }
    public void setDuelWins(int duelWins) { this.duelWins = duelWins; }
    public int getHighestArcadeScore() { return highestArcadeScore; }
    public void setHighestArcadeScore(int highestArcadeScore) { this.highestArcadeScore = highestArcadeScore; }
    public boolean isNewPlayer() { return isNewPlayer; }
    public void setNewPlayer(boolean newPlayer) { isNewPlayer = newPlayer; }
    public int getStarsCaught() { return starsCaught; }
    public void setStarsCaught(int starsCaught) { this.starsCaught = starsCaught; }
    public int getBestCombo() { return bestCombo; }
    public void setBestCombo(int bestCombo) { this.bestCombo = bestCombo; }
    public int getTotalStarDust() { return totalStarDust; }
    public void setTotalStarDust(int totalStarDust) { this.totalStarDust = totalStarDust; }

}
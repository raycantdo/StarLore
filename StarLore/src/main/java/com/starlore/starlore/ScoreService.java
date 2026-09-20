package com.starlore.starlore;

public class ScoreService {

    private static final PlayerDAO playerDAO = new PlayerDAO();

    /**
     * Adds StarDust to the player's current total
     * and permanently saves the new total to MySQL.
     */
    public static void addStarDust(Player player, int amount) {
        if (player == null || amount <= 0) return;

        int newTotal = player.getTotalStarDust() + amount;

        player.setTotalStarDust(newTotal);

        playerDAO.updateStarDust(
                player.getUsername(),
                newTotal
        );
    }

    /**
     * Spends StarDust if the player has enough.
     *
     * @return true if the StarDust was successfully spent
     */
    public static boolean spendStarDust(Player player, int amount) {
        if (player == null || amount <= 0) return false;

        int current = player.getTotalStarDust();

        if (current < amount) {
            return false;
        }

        int newTotal = current - amount;

        player.setTotalStarDust(newTotal);

        playerDAO.updateStarDust(
                player.getUsername(),
                newTotal
        );

        return true;
    }
}
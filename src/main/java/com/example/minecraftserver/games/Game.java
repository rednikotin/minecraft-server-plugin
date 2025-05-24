package com.example.minecraftserver.games;

import org.bukkit.entity.Player;

public interface Game {
    void startGame();
    void endGame(boolean force);
    void addPlayer(Player player);
    void removePlayer(Player player);
    boolean isPlayerInGame(Player player);
    String getGameName();
    boolean canJoin();
    GameState getGameState();
}

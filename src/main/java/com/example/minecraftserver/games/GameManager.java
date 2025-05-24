package com.example.minecraftserver.games;

import com.example.minecraftserver.Main;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GameManager {
    private final Main plugin;
    private final Map<String, Game> registeredGames = new HashMap<>();
    private final Map<UUID, Game> playerGames = new HashMap<>();

    public GameManager(Main plugin) {
        this.plugin = plugin;
    }

    public void registerGame(String name, Game game) {
        registeredGames.put(name.toLowerCase(), game);
        plugin.getLogger().info("Game registered: " + name);
    }

    public Game getGame(String name) {
        return registeredGames.get(name.toLowerCase());
    }

    public void joinGame(Player player, String gameName) {
        if (isPlayerInAnyGame(player)) {
            player.sendMessage("§cYou are already in a game. Please leave it first.");
            return;
        }

        Game game = getGame(gameName);
        if (game == null) {
            player.sendMessage("§cGame '" + gameName + "' not found.");
            return;
        }

        if (game.canJoin()) {
            game.addPlayer(player);
            playerGames.put(player.getUniqueId(), game);
            player.sendMessage("§aYou have joined " + game.getGameName() + "!");
            plugin.getLogger().info("Player " + player.getName() + " joined game " + game.getGameName());
        } else {
            player.sendMessage("§cCould not join " + game.getGameName() + ". It might be full or in progress.");
        }
    }

    public void leaveGame(Player player) {
        Game game = getPlayerGame(player);
        if (game != null) {
            game.removePlayer(player);
            playerGames.remove(player.getUniqueId());
            player.sendMessage("§aYou have left " + game.getGameName() + ".");
            plugin.getLogger().info("Player " + player.getName() + " left game " + game.getGameName());
        } else {
            player.sendMessage("§cYou are not currently in any game.");
        }
    }

    public Game getPlayerGame(Player player) {
        return playerGames.get(player.getUniqueId());
    }

    public boolean isPlayerInAnyGame(Player player) {
        return playerGames.containsKey(player.getUniqueId());
    }

    public java.util.Set<String> getRegisteredGameNames() {
        return registeredGames.keySet();
    }

    public void shutdown() {
        plugin.getLogger().info("Forcing all games to end due to plugin disabling...");
        for (Game game : registeredGames.values()) {
            if (game.getGameState() == GameState.ACTIVE || 
                game.getGameState() == GameState.STARTING || 
                game.getGameState() == GameState.ENDING) {
                plugin.getLogger().info("Forcefully ending game: " + game.getGameName());
                game.endGame(true); // true for forceful shutdown
            }
        }
        // Clear player tracking after all games have attempted to end their players
        playerGames.clear();
        plugin.getLogger().info("All games ended and player tracking cleared.");
    }
}

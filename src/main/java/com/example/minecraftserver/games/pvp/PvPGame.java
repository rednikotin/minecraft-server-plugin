package com.example.minecraftserver.games.pvp;

import com.example.minecraftserver.Main;
import com.example.minecraftserver.games.Game;
import com.example.minecraftserver.games.GameState;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class PvPGame implements Game, Listener {

    private final Main plugin;
    private final String gameName = "PvP 1-1";
    private GameState gameState = GameState.WAITING;
    private final List<Player> players = new ArrayList<>();
    private Location arenaSpawnPoint1;
    private Location arenaSpawnPoint2;
    private Location lobbySpawnPoint;
    private final Map<UUID, PlayerState> playerOriginalStates = new HashMap<>();

    private static class PlayerState {
        ItemStack[] inventoryContents;
        ItemStack[] armorContents;
        double health;
        int foodLevel;
        float saturation;
        int level;
        float exp;
        GameMode gameMode;
        Collection<PotionEffect> potionEffects;

        PlayerState(Player player) {
            this.inventoryContents = player.getInventory().getContents();
            this.armorContents = player.getInventory().getArmorContents();
            this.health = player.getHealth(); // As per new spec
            this.foodLevel = player.getFoodLevel();
            this.saturation = player.getSaturation();
            this.level = player.getLevel();
            this.exp = player.getExp();
            this.gameMode = player.getGameMode();
            this.potionEffects = new ArrayList<>(player.getActivePotionEffects());
        }

        void restore(Player player) {
            player.getInventory().setContents(inventoryContents);
            player.getInventory().setArmorContents(armorContents);
            player.setHealth(health); // As per new spec
            player.setFoodLevel(foodLevel);
            player.setSaturation(saturation);
            player.setLevel(level);
            player.setExp(exp);
            player.setGameMode(gameMode);
            player.getActivePotionEffects().forEach(effect -> player.removePotionEffect(effect.getType()));
            if (this.potionEffects != null) {
                player.addPotionEffects(this.potionEffects);
            }
        }
    }

    private Location loadLocationFromString(Main plugin, World world, String locString, Location defaultLocation) {
        if (world == null) {
            plugin.getLogger().severe("Cannot load location: World is null. Using default location provided (which might also be null).");
            return defaultLocation;
        }
        if (locString == null || locString.isEmpty()) {
            plugin.getLogger().warning("Location string is null or empty. Using default.");
            return defaultLocation;
        }
        String[] parts = locString.split(",");
        if (parts.length != 5) { // x,y,z,yaw,pitch
            plugin.getLogger().warning("Invalid location string format: '" + locString + "'. Expected 5 parts (x,y,z,yaw,pitch). Using default.");
            return defaultLocation;
        }
        try {
            double x = Double.parseDouble(parts[0].trim());
            double y = Double.parseDouble(parts[1].trim());
            double z = Double.parseDouble(parts[2].trim());
            float yaw = Float.parseFloat(parts[3].trim());
            float pitch = Float.parseFloat(parts[4].trim());
            return new Location(world, x, y, z, yaw, pitch);
        } catch (NumberFormatException e) {
            plugin.getLogger().warning("Error parsing location string '" + locString + "': " + e.getMessage() + ". Using default.");
            return defaultLocation;
        }
    }

    public PvPGame(Main plugin) {
        this.plugin = plugin;
        org.bukkit.configuration.file.FileConfiguration config = plugin.getConfig();

        String pvpWorldName = config.getString("pvp.world", "world");
        World pvpWorld = plugin.getServer().getWorld(pvpWorldName);

        if (pvpWorld == null) {
            plugin.getLogger().warning("PvP world '" + pvpWorldName + "' not found! Attempting to use server's primary world.");
            if (!plugin.getServer().getWorlds().isEmpty()) {
                pvpWorld = plugin.getServer().getWorlds().get(0);
                plugin.getLogger().info("Using primary world: " + pvpWorld.getName() + " for PvP.");
            } else {
                plugin.getLogger().severe("CRITICAL: No worlds found on the server. PvP game cannot be initialized.");
                this.arenaSpawnPoint1 = null;
                this.arenaSpawnPoint2 = null;
                this.lobbySpawnPoint = null;
                return; // Essential to prevent further errors
            }
        }
        
        // Define default locations using the determined pvpWorld (which must be non-null here if we didn't return)
        Location defaultLobbySpawn = new Location(pvpWorld, 0.5, 65.0, 0.5, 0, 0);
        Location defaultArenaSpawn1 = new Location(pvpWorld, 100.5, 65.0, 100.5, 90, 0);
        Location defaultArenaSpawn2 = new Location(pvpWorld, 110.5, 65.0, 100.5, -90, 0);

        this.lobbySpawnPoint = loadLocationFromString(plugin, pvpWorld, config.getString("pvp.lobby_spawn"), defaultLobbySpawn);
        this.arenaSpawnPoint1 = loadLocationFromString(plugin, pvpWorld, config.getString("pvp.arena_spawn1"), defaultArenaSpawn1);
        this.arenaSpawnPoint2 = loadLocationFromString(plugin, pvpWorld, config.getString("pvp.arena_spawn2"), defaultArenaSpawn2);

        if (this.arenaSpawnPoint1 == null || this.arenaSpawnPoint2 == null || this.lobbySpawnPoint == null) {
            plugin.getLogger().severe("One or more critical PvP spawn points failed to load. PvP game might be unjoinable or behave unexpectedly.");
            // Existing canJoin() checks for arenaSpawnPoint1 != null will handle joinability.
        }
    }

    @Override
    public void addPlayer(Player player) {
        if (arenaSpawnPoint1 == null) {
            player.sendMessage("§cPvP game is currently disabled due to an internal error."); // Exact message
            return;
        }
        if (gameState != GameState.WAITING || players.size() >= 2) {
            player.sendMessage("§cGame is not available or full."); // Exact message
            return;
        }

        playerOriginalStates.put(player.getUniqueId(), new PlayerState(player));
        player.getInventory().clear();
        player.getActivePotionEffects().forEach(effect -> player.removePotionEffect(effect.getType()));
        player.setHealth(player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());
        player.setFoodLevel(20);
        player.setSaturation(20f);
        player.setGameMode(GameMode.SURVIVAL);

        players.add(player);
        player.teleport(players.size() == 1 ? arenaSpawnPoint1 : arenaSpawnPoint2);
        plugin.getServer().broadcastMessage("§e" + player.getName() + " has joined PvP! " + (players.size() == 1 ? "Waiting for one more player." : "Match will start soon!"));

        if (players.size() == 2) {
            startGame();
        }
    }

    @Override
    public void removePlayer(Player player) {
        boolean wasInGame = players.remove(player);
        if (!wasInGame) return;

        PlayerState originalState = playerOriginalStates.remove(player.getUniqueId());
        if (player.isOnline()) {
            if (originalState != null) {
                originalState.restore(player);
            }
            if (lobbySpawnPoint != null) {
                player.teleport(lobbySpawnPoint);
            }
        }
        plugin.getServer().broadcastMessage("§e" + player.getName() + " has left PvP.");
    }

    @Override
    public void startGame() {
        if (arenaSpawnPoint1 == null || players.size() != 2) {
            gameState = GameState.WAITING;
            plugin.getServer().broadcastMessage("§cPvP cannot start. Conditions not met (not enough players or arena not set)."); // Exact message and logic
            if (!players.isEmpty()) { // Added this check as per spec - though my old code had it
                 endGame(true);
            }
            return;
        }

        gameState = GameState.STARTING;
        plugin.getServer().broadcastMessage("§aPvP Match starting in 5 seconds! Get ready!");

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (players.size() != 2) { // Check again if a player left during countdown
                gameState = GameState.WAITING;
                plugin.getServer().broadcastMessage("§cPvP start cancelled as a player left.");
                endGame(true); // Force end to reset any remaining player
                return;
            }
            gameState = GameState.ACTIVE;
            Player p1 = players.get(0);
            Player p2 = players.get(1);

            // Ensure players are still valid and online (already had this, ensuring it's same)
            if (p1 == null || !p1.isOnline() || p2 == null || !p2.isOnline()) {
                 plugin.getServer().broadcastMessage("§cPvP start cancelled as a player is no longer available."); // Exact message
                 endGame(true);
                 return;
            }

            p1.teleport(arenaSpawnPoint1);
            p2.teleport(arenaSpawnPoint2);

            setupPlayerGear(p1);
            setupPlayerGear(p2);

            plugin.getServer().broadcastMessage("§c§lFIGHT!");
        }, 5 * 20L); // 5 seconds * 20 ticks/second
    }

    private void setupPlayerGear(Player player) {
        player.getInventory().clear();
        player.getInventory().setItem(0, new ItemStack(Material.IRON_SWORD));
        player.getInventory().setItem(1, new ItemStack(Material.BOW));
        player.getInventory().setItem(9, new ItemStack(Material.ARROW, 16)); // Slot 9 for off-hand or first inventory slot after hotbar
        player.getInventory().setHelmet(new ItemStack(Material.IRON_HELMET));
        player.getInventory().setChestplate(new ItemStack(Material.IRON_CHESTPLATE));
        player.getInventory().setLeggings(new ItemStack(Material.IRON_LEGGINGS));
        player.getInventory().setBoots(new ItemStack(Material.IRON_BOOTS));
        player.setHealth(player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());
        player.setFoodLevel(20);
        player.setSaturation(20f);
    }

    @Override
    public void endGame(boolean force) {
        GameState previousState = gameState; // Exact variable name
        gameState = GameState.ENDING; // Exact variable name

        if (force && previousState == GameState.ACTIVE) {
            plugin.getServer().broadcastMessage("§ePvP match ended forcefully."); // Exact message
        }
        
        List<Player> playersToProcess = new ArrayList<>(players); // Exact variable name
        for (Player p : playersToProcess) {
            removePlayer(p); 
        }

        Set<UUID> remainingStateUUIDs = new HashSet<>(playerOriginalStates.keySet()); // Exact variable name
        for (UUID uuid : remainingStateUUIDs) {
            Player p = plugin.getServer().getPlayer(uuid);
            if (p != null && p.isOnline()) {
                PlayerState state = playerOriginalStates.remove(uuid);
                if (state != null) state.restore(p); // Exact logic
                if (lobbySpawnPoint != null) p.teleport(lobbySpawnPoint);
            } else {
                playerOriginalStates.remove(uuid); // Exact logic
            }
        }
        
        players.clear(); 
        playerOriginalStates.clear();

        gameState = GameState.WAITING; // Exact variable name
        if (previousState == GameState.ACTIVE || previousState == GameState.ENDING || (force && previousState != GameState.WAITING) ) { // Exact condition
             plugin.getServer().broadcastMessage("§aPvP arena is now open for new challengers."); // Exact message
        }
    }

    @Override
    public boolean isPlayerInGame(Player player) {
        return players.contains(player);
    }

    @Override
    public String getGameName() {
        return this.gameName;
    }

    @Override
    public boolean canJoin() {
        return gameState == GameState.WAITING && players.size() < 2 && arenaSpawnPoint1 != null;
    }

    @Override
    public GameState getGameState() {
        return this.gameState;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        if (!isPlayerInGame(victim) || gameState != GameState.ACTIVE) {
            return;
        }

        event.getDrops().clear();
        event.setDroppedExp(0);

        Player killer = victim.getKiller();
        
        // Important: removePlayer will handle state restoration and teleportation
        // It also modifies the 'players' list.
        // We call it *before* determining the winner to ensure player list is accurate.
        removePlayer(victim); 

        plugin.getServer().broadcastMessage("§c" + victim.getName() + 
            (killer != null && isPlayerInGame(killer) ? " was slain by " + killer.getName() : " was eliminated") + "!");

        // Check game state *after* removing the player
        if (players.size() == 1 && gameState == GameState.ACTIVE) { // Exact logic
            Player winner = players.get(0);
            plugin.getServer().broadcastMessage("§a" + winner.getName() + " wins the PvP match!");
        } else if (players.isEmpty() && gameState == GameState.ACTIVE) { // Exact logic
            plugin.getServer().broadcastMessage("§eThe match ends in a draw!");
        }
        if (gameState == GameState.ACTIVE) endGame(false); // Exact logic
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (!isPlayerInGame(player)) {
            return;
        }

        boolean wasActive = gameState == GameState.ACTIVE; // Exact logic
        
        playerOriginalStates.remove(player.getUniqueId()); // Exact logic
        boolean removed = players.remove(player); 

        if (!removed) return; 

        plugin.getServer().broadcastMessage("§e" + player.getName() + " has quit and left the PvP match."); // Exact message

        if (wasActive) {
            if (players.size() == 1) {
                Player winner = players.get(0);
                plugin.getServer().broadcastMessage("§a" + winner.getName() + " wins as their opponent quit!"); // Exact message
            } else {
                plugin.getServer().broadcastMessage("§eThe match ended as a player quit."); // Exact message
            }
            endGame(false); 
        } else if (gameState == GameState.WAITING || gameState == GameState.STARTING) { // Exact logic
            if (players.isEmpty()) {
                gameState = GameState.WAITING; 
            }
            plugin.getServer().broadcastMessage("§cPvP match cannot continue as a player quit."); // Exact message
        }
    }
}

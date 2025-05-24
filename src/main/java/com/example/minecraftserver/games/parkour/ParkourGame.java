package com.example.minecraftserver.games.parkour;

import com.example.minecraftserver.Main;
import com.example.minecraftserver.games.Game;
import com.example.minecraftserver.games.GameState;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class ParkourGame implements Game, Listener {

    private final Main plugin;
    private final String gameName = "Parkour";
    private GameState gameState = GameState.WAITING;
    private final Map<Player, ParkourPlayerData> activePlayers = new HashMap<>();
    private Location startPoint;
    private List<Location> checkpoints;
    private Location endPoint;
    private Location lobbySpawnPoint;
    private final Map<UUID, ParkourPlayerState> playerOriginalStates = new HashMap<>();

    private static class ParkourPlayerState {
        Location originalLocation;
        GameMode originalGameMode;
        ItemStack[] inventoryContents;
        ItemStack[] armorContents;
        float originalFlySpeed;
        boolean originalAllowFlight;

        ParkourPlayerState(Player player) {
            this.originalLocation = player.getLocation();
            this.originalGameMode = player.getGameMode();
            this.inventoryContents = player.getInventory().getContents();
            this.armorContents = player.getInventory().getArmorContents();
            this.originalFlySpeed = player.getFlySpeed();
            this.originalAllowFlight = player.getAllowFlight();
        }

        void restore(Player player) {
            player.teleport(originalLocation);
            player.setGameMode(originalGameMode);
            player.getInventory().setContents(inventoryContents);
            player.getInventory().setArmorContents(armorContents);
            player.setFlySpeed(originalFlySpeed);
            player.setAllowFlight(originalAllowFlight);
        }
    }

    private static class ParkourPlayerData {
        long startTime;
        int currentCheckpoint; // -1 for start
        Location lastCheckpointLocation;

        ParkourPlayerData(long startTime, Location startLoc) {
            this.startTime = startTime;
            this.currentCheckpoint = -1;
            this.lastCheckpointLocation = startLoc;
        }
    }

    private Location loadLocationFromString(Main plugin, World world, String locString, Location defaultLocation) {
        if (world == null) {
            plugin.getLogger().severe("Cannot load location: World is null. Using default location provided (which might also be null).");
            return defaultLocation;
        }
        if (locString == null || locString.isEmpty()) {
            plugin.getLogger().warning("Location string is null or empty for parkour. Using default.");
            return defaultLocation;
        }
        String[] parts = locString.split(",");
        if (parts.length != 5) { // x,y,z,yaw,pitch
            plugin.getLogger().warning("Invalid parkour location string format: '" + locString + "'. Expected 5 parts (x,y,z,yaw,pitch). Using default.");
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
            plugin.getLogger().warning("Error parsing parkour location string '" + locString + "': " + e.getMessage() + ". Using default.");
            return defaultLocation;
        }
    }

    public ParkourGame(Main plugin) {
        this.plugin = plugin;
        org.bukkit.configuration.file.FileConfiguration config = plugin.getConfig();

        String parkourWorldName = config.getString("parkour.world", "world");
        World parkourWorld = plugin.getServer().getWorld(parkourWorldName);

        if (parkourWorld == null) {
            plugin.getLogger().warning("Parkour world '" + parkourWorldName + "' not found! Attempting to use server's primary world.");
            if (!plugin.getServer().getWorlds().isEmpty()) {
                parkourWorld = plugin.getServer().getWorlds().get(0);
                plugin.getLogger().info("Using primary world: " + parkourWorld.getName() + " for Parkour.");
            } else {
                plugin.getLogger().severe("CRITICAL: No worlds found on the server. Parkour game cannot be initialized.");
                this.startPoint = null;
                this.checkpoints = new ArrayList<>();
                this.endPoint = null;
                this.lobbySpawnPoint = null;
                this.gameState = GameState.INACTIVE;
                return; 
            }
        }

        Location defaultLobby = new Location(parkourWorld, 0.5, 65.0, 0.5, 0, 0);
        Location defaultStart = new Location(parkourWorld, 50.5, 65.0, 50.5, 0, 0);
        Location defaultEnd = new Location(parkourWorld, 80.5, 70.0, 60.5, 0, 0);

        this.lobbySpawnPoint = loadLocationFromString(plugin, parkourWorld, config.getString("parkour.lobby_spawn"), defaultLobby);
        this.startPoint = loadLocationFromString(plugin, parkourWorld, config.getString("parkour.start_point"), defaultStart);
        this.endPoint = loadLocationFromString(plugin, parkourWorld, config.getString("parkour.end_point"), defaultEnd);
        
        this.checkpoints = new ArrayList<>();
        List<String> checkpointStrings = config.getStringList("parkour.checkpoints");
        if (checkpointStrings == null || checkpointStrings.isEmpty()) {
            plugin.getLogger().warning("Parkour checkpoints not defined in config or empty. Using hardcoded defaults if available or none.");
            // Add hardcoded default checkpoint strings if desired, or rely on empty list if none should be default
             checkpointStrings = Arrays.asList("60.5,67.0,50.5,0,0", "70.5,65.0,60.5,0,0"); // Default checkpoints as strings
        }

        for (String cpString : checkpointStrings) {
            // For checkpoints, a null defaultLocation means a failed checkpoint is skipped
            Location cp = loadLocationFromString(plugin, parkourWorld, cpString, null); 
            if (cp != null) {
                this.checkpoints.add(cp);
            } else {
                plugin.getLogger().warning("Failed to load parkour checkpoint from string: '" + cpString + "'. It will be skipped.");
            }
        }

        if (this.startPoint == null || this.endPoint == null) {
            plugin.getLogger().severe("Critical Parkour points (start or end) failed to load. Parkour game will be disabled.");
            this.gameState = GameState.INACTIVE;
        } else if (this.checkpoints.isEmpty()) {
             plugin.getLogger().warning("Parkour game has no checkpoints loaded. Players will go from start to end directly.");
             this.gameState = GameState.WAITING; // Still playable
        }
        else {
            this.gameState = GameState.WAITING;
        }
    }

    @Override
    public void addPlayer(Player player) {
        if (gameState == GameState.INACTIVE || startPoint == null) {
            player.sendMessage("§cParkour is currently disabled.");
            return;
        }
        if (activePlayers.containsKey(player)) {
            player.sendMessage("§cYou are already in the parkour.");
            return;
        }

        playerOriginalStates.put(player.getUniqueId(), new ParkourPlayerState(player));

        player.setGameMode(GameMode.ADVENTURE);
        player.getInventory().clear();
        player.getInventory().setArmorContents(new ItemStack[4]); // Clear armor
        player.setAllowFlight(false);
        player.setFlying(false);
        // Restore health and food
        player.setHealth(player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());
        player.setFoodLevel(20);
        player.setSaturation(5f); // Decent saturation

        player.teleport(startPoint);
        activePlayers.put(player, new ParkourPlayerData(System.currentTimeMillis(), startPoint));
        player.sendMessage("§aParkour started! Reach the end. Type §e/game leave parkour §ato quit.");

        if (gameState == GameState.WAITING) {
            gameState = GameState.ACTIVE;
        }
    }

    @Override
    public void removePlayer(Player player) {
        if (!activePlayers.containsKey(player)) {
            return;
        }

        ParkourPlayerState originalState = playerOriginalStates.remove(player.getUniqueId());
        if (player.isOnline()) {
            if (originalState != null) {
                originalState.restore(player);
            } else {
                // Fallback if state was somehow lost
                if (lobbySpawnPoint != null) player.teleport(lobbySpawnPoint);
                player.setGameMode(GameMode.SURVIVAL); // Default gamemode
            }
        }
        activePlayers.remove(player);
        player.sendMessage("§eYou have left the parkour.");

        if (activePlayers.isEmpty()) {
            gameState = GameState.WAITING;
        }
    }

    @Override
    public void startGame() {
        plugin.getLogger().info("ParkourGame.startGame() called, but parkour starts individually.");
        // Parkour starts when a player joins, not a session start for all.
    }

    @Override
    public void endGame(boolean force) {
        if (force) {
            plugin.getServer().broadcastMessage("§cParkour game is being forcefully reset/ended.");
        }
        List<Player> currentPlayers = new ArrayList<>(activePlayers.keySet());
        for (Player p : currentPlayers) {
            removePlayer(p); // This handles restore and teleport
        }
        activePlayers.clear(); // Ensure map is empty
        gameState = GameState.WAITING;
    }

    @Override
    public boolean isPlayerInGame(Player player) {
        return activePlayers.containsKey(player);
    }

    @Override
    public String getGameName() {
        return this.gameName;
    }

    @Override
    public boolean canJoin() {
        return gameState != GameState.INACTIVE && startPoint != null;
    }

    @Override
    public GameState getGameState() {
        return this.gameState;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!activePlayers.containsKey(player)) {
            return;
        }

        ParkourPlayerData data = activePlayers.get(player);
        Location to = event.getTo();
        if (to == null) return; // Should not happen

        // Prevent movement for a very short duration after teleport to checkpoint to avoid immediate re-trigger
        if (System.currentTimeMillis() - data.startTime < 500 && data.currentCheckpoint > -1) {
             // This check is a bit naive, better would be to store last teleport time to checkpoint
        }


        // Check for falling into void
        // Using hardcoded 0 for void_level as config isn't fully set up yet for this subtask
        if (to.getY() < plugin.getConfig().getDouble("parkour.void_level", 0.0)) {
            player.teleport(data.lastCheckpointLocation);
            player.sendMessage("§cOuch! Back to your last checkpoint.");
            // Restore health/food after fall
            player.setHealth(player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());
            player.setFoodLevel(20);
            return;
        }

        // Check for reaching next checkpoint
        int nextCheckpointIndex = data.currentCheckpoint + 1;
        if (nextCheckpointIndex < checkpoints.size()) {
            Location nextCpLoc = checkpoints.get(nextCheckpointIndex);
            if (isWithinBlock(to, nextCpLoc)) {
                data.currentCheckpoint = nextCheckpointIndex;
                data.lastCheckpointLocation = nextCpLoc;
                player.sendMessage("§aCheckpoint " + (nextCheckpointIndex + 1) + " reached!");
                // Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "playsound minecraft:entity.player.levelup master " + player.getName() + " ~ ~ ~");
            }
        }
        // Check for reaching end point (only if all checkpoints are done)
        else if (data.currentCheckpoint == checkpoints.size() - 1 && isWithinBlock(to, endPoint)) {
            long timeTaken = System.currentTimeMillis() - data.startTime;
            player.sendMessage("§6§lCongratulations! §eYou finished the parkour in §a" + formatTime(timeTaken) + "§e!");
            // Bukkit.getServer().broadcastMessage("§6" + player.getName() + " finished the parkour in " + formatTime(timeTaken) + "!");
            removePlayer(player); // This handles restore and teleport
        }
    }

    private boolean isWithinBlock(Location loc1, Location loc2) {
        if (loc1 == null || loc2 == null || loc1.getWorld() == null || loc2.getWorld() == null) return false;
        return loc1.getWorld().equals(loc2.getWorld()) &&
               loc1.getBlockX() == loc2.getBlockX() &&
               loc1.getBlockY() == loc2.getBlockY() &&
               loc1.getBlockZ() == loc2.getBlockZ();
    }

    private String formatTime(long millis) {
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60;
        long hundredths = (millis % 1000) / 10; // Get hundredths of a second
        return String.format("%d min, %d.%02d sec", minutes, seconds, hundredths);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (activePlayers.containsKey(player)) {
            // State needs to be removed, but player object is invalid for restore
            playerOriginalStates.remove(player.getUniqueId());
            activePlayers.remove(player);
            plugin.getLogger().info(player.getName() + " quit during parkour. Their state was cleared.");
            if (activePlayers.isEmpty()) {
                gameState = GameState.WAITING;
            }
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        if (!activePlayers.containsKey(player)) {
            return;
        }

        event.getDrops().clear();
        event.setDroppedExp(0);
        event.setKeepInventory(true); // Important for parkour

        // Teleport back to last checkpoint and restore stats AFTER respawn
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline() && activePlayers.containsKey(player)) { // Check if still online and in game
                // Spigot API to force respawn without player clicking button
                // This helps in making the process smoother.
                // If not on Spigot/Paper, player has to click respawn button.
                try {
                    player.spigot().respawn();
                } catch (NoSuchMethodError e) {
                    plugin.getLogger().info("player.spigot().respawn() not available. Player will need to manually respawn.");
                }

                ParkourPlayerData data = activePlayers.get(player);
                player.teleport(data.lastCheckpointLocation);
                player.setHealth(player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());
                player.setFoodLevel(20);
                player.setFireTicks(0); // Extinguish fire
                player.getActivePotionEffects().forEach(effect -> player.removePotionEffect(effect.getType())); // Clear effects
                player.sendMessage("§cYou died! Returned to your last checkpoint.");
            }
        }, 1L); // Delay 1 tick to allow death event to process and respawn to occur.
    }
}

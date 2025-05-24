package com.example.minecraftserver;

import com.example.minecraftserver.commands.GameCommands; // Added import
import com.example.minecraftserver.games.GameManager;
import com.example.minecraftserver.games.parkour.ParkourGame;
import com.example.minecraftserver.games.pvp.PvPGame;
import com.example.minecraftserver.menu.MenuManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.java.JavaPluginCommand; // Added import

import java.util.logging.Logger;

public class Main extends JavaPlugin {
    private static final Logger LOGGER = Logger.getLogger("MinecraftServer");
    private MenuManager menuManager;
    private GameManager gameManager;

    @Override
    public void onEnable() {
        saveDefaultConfig(); 
        LOGGER.info("MinecraftServer plugin has been enabled!");
        // Plugin startup logic

        // Initialize GameManager (must be before MenuManager)
        gameManager = new GameManager(this);

        // Initialize and register MenuManager (pass GameManager)
        menuManager = new MenuManager(this.gameManager); 
        getServer().getPluginManager().registerEvents(menuManager, this);

        // Register PvPGame
        PvPGame pvpGame = new PvPGame(this);
        if (gameManager != null) {
            gameManager.registerGame("pvp", pvpGame);
        }
        getServer().getPluginManager().registerEvents(pvpGame, this);
        if (!pvpGame.canJoin()) {
            getLogger().warning("PvP Game is not joinable immediately after creation. Check for errors like missing world 'world'.");
        }

        // Register ParkourGame
        ParkourGame parkourGame = new ParkourGame(this);
        if (gameManager != null) {
            gameManager.registerGame("parkour", parkourGame);
        }
        getServer().getPluginManager().registerEvents(parkourGame, this);
        if (!parkourGame.canJoin()) {
            getLogger().warning("Parkour Game is not joinable (e.g. world 'world' missing or course not set up).");
        }

        // Register GameCommands (ensure menuManager and gameManager are available)
        GameCommands gameCommands = new GameCommands(this, this.getMenuManager(), this.getGameManager());
        JavaPluginCommand mainGameCommand = getCommand("game");
        if (mainGameCommand != null) {
            mainGameCommand.setExecutor(gameCommands);
            mainGameCommand.setTabCompleter(gameCommands);
        } else {
            getLogger().severe("Command 'game' not found in plugin.yml! Check the plugin.yml file and command name.");
        }
    }

    @Override
    public void onDisable() {
        LOGGER.info("MinecraftServer plugin is being disabled..."); // Changed message slightly for clarity

        if (gameManager != null) {
            gameManager.shutdown();
        }

        LOGGER.info("MinecraftServer plugin has been disabled!");
        // Further plugin shutdown logic if any
    }

    // Getter for MenuManager if other parts of the plugin need access to it
    public MenuManager getMenuManager() {
        return menuManager;
    }

    // Getter for GameManager
    public GameManager getGameManager() {
        return gameManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("games") && sender instanceof Player) {
            gameMenu.open((Player) sender);
            return true;
        }
        return false;
    }
}

package com.example.minecraftserver;

import com.example.minecraftserver.game.GameMenu;
import com.example.minecraftserver.game.PvPGameManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;

public class Main extends JavaPlugin {
    private static final Logger LOGGER = Logger.getLogger("MinecraftServer");
    private GameMenu gameMenu;

    @Override
    public void onEnable() {
        LOGGER.info("MinecraftServer plugin has been enabled!");
        gameMenu = new GameMenu();
        getServer().getPluginManager().registerEvents(gameMenu, this);
        getServer().getPluginManager().registerEvents(PvPGameManager.getInstance(), this);
        // No additional startup logic
    }

    @Override
    public void onDisable() {
        LOGGER.info("MinecraftServer plugin has been disabled!");
        // Plugin shutdown logic
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

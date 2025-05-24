package com.example.minecraftserver;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Logger;

public class Main extends JavaPlugin {
    private static final Logger LOGGER = Logger.getLogger("MinecraftServer");

    @Override
    public void onEnable() {
        LOGGER.info("MinecraftServer plugin has been enabled!");
        // Plugin startup logic
    }

    @Override
    public void onDisable() {
        LOGGER.info("MinecraftServer plugin has been disabled!");
        // Plugin shutdown logic
    }
}

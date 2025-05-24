package com.example.minecraftserver.game;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class ParkourManager {
    private static final Location PARKOUR_START;

    static {
        World world = Bukkit.getWorld("world");
        PARKOUR_START = new Location(world, 20, 80, 0);
    }

    public static void teleportToStart(Player player) {
        player.teleport(PARKOUR_START);
    }
}

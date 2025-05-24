package com.example.minecraftserver.game;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import java.util.LinkedList;
import java.util.Queue;

public class PvPGameManager implements Listener {
    private static final PvPGameManager instance = new PvPGameManager();
    private final Queue<Player> queue = new LinkedList<>();
    private Player player1;
    private Player player2;

    private final Location arena1;
    private final Location arena2;
    private final Location lobby;

    private PvPGameManager() {
        World world = Bukkit.getWorld("world");
        arena1 = new Location(world, 0, 100, 0);
        arena2 = new Location(world, 5, 100, 0);
        lobby = new Location(world, 0, 80, 0);
    }

    public static PvPGameManager getInstance() {
        return instance;
    }

    public void joinQueue(Player player) {
        if (player1 != null && player1.equals(player) || player2 != null && player2.equals(player)) {
            player.sendMessage(ChatColor.RED + "You are already in a duel!");
            return;
        }
        queue.add(player);
        player.sendMessage(ChatColor.GREEN + "Joined the 1v1 queue. Waiting for opponent...");
        if (queue.size() >= 2) {
            startDuel();
        }
    }

    private void startDuel() {
        player1 = queue.poll();
        player2 = queue.poll();
        if (player1 == null || player2 == null) {
            return;
        }
        preparePlayer(player1, arena1);
        preparePlayer(player2, arena2);
        Bukkit.broadcastMessage(ChatColor.GOLD + "Duel started between " + player1.getName() + " and " + player2.getName() + "!");
    }

    private void preparePlayer(Player player, Location loc) {
        player.getInventory().clear();
        player.setGameMode(GameMode.SURVIVAL);
        player.teleport(loc);
        player.getInventory().addItem(new ItemStack(Material.IRON_SWORD));
        player.setHealth(20);
        player.setFoodLevel(20);
    }

    private void endDuel(Player winner, Player loser) {
        Bukkit.broadcastMessage(ChatColor.AQUA + winner.getName() + " won the duel against " + loser.getName() + "!");
        resetPlayer(winner);
        resetPlayer(loser);
        player1 = null;
        player2 = null;
    }

    private void resetPlayer(Player player) {
        player.getInventory().clear();
        player.teleport(lobby);
        player.setHealth(20);
        player.setFoodLevel(20);
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player dead = event.getEntity();
        Player killer = dead.getKiller();
        if (dead.equals(player1) && killer != null && killer.equals(player2)) {
            endDuel(player2, player1);
        } else if (dead.equals(player2) && killer != null && killer.equals(player1)) {
            endDuel(player1, player2);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player quitter = event.getPlayer();
        if (quitter.equals(player1) && player2 != null) {
            endDuel(player2, player1);
        } else if (quitter.equals(player2) && player1 != null) {
            endDuel(player1, player2);
        } else {
            queue.remove(quitter);
        }
    }
}

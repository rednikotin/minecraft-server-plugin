package com.example.minecraftserver.menu;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import com.example.minecraftserver.games.GameManager; // Added import

import java.util.Arrays;

public class MenuManager implements Listener {

    private final GameManager gameManager; // Added field

    // Updated constructor
    public MenuManager(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    public void openMainMenu(Player player) {
        Inventory menu = Bukkit.createInventory(null, 9, "Game Menu");

        // PvP 1-1 Item
        ItemStack pvpItem = new ItemStack(Material.DIAMOND_SWORD);
        ItemMeta pvpMeta = pvpItem.getItemMeta();
        if (pvpMeta != null) {
            pvpMeta.setDisplayName("§c§lPvP 1-1");
            pvpMeta.setLore(Arrays.asList("§7Click to join a 1v1 PvP match!"));
            pvpItem.setItemMeta(pvpMeta);
        }

        // Parkour Item
        ItemStack parkourItem = new ItemStack(Material.FEATHER);
        ItemMeta parkourMeta = parkourItem.getItemMeta();
        if (parkourMeta != null) {
            parkourMeta.setDisplayName("§a§lParkour");
            parkourMeta.setLore(Arrays.asList("§7Click to start a parkour challenge!"));
            parkourItem.setItemMeta(parkourMeta);
        }

        menu.setItem(3, pvpItem);
        menu.setItem(5, parkourItem);

        player.openInventory(menu);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals("Game Menu")) {
            return;
        }

        event.setCancelled(true);

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || clickedItem.getType() == Material.AIR) {
            return;
        }

        Player player = (Player) event.getWhoClicked();

        if (clickedItem.getType() == Material.DIAMOND_SWORD && clickedItem.hasItemMeta() && clickedItem.getItemMeta().getDisplayName().equals("§c§lPvP 1-1")) {
            gameManager.joinGame(player, "pvp"); // Updated to join game
            player.closeInventory();
        } else if (clickedItem.getType() == Material.FEATHER && clickedItem.hasItemMeta() && clickedItem.getItemMeta().getDisplayName().equals("§a§lParkour")) {
            gameManager.joinGame(player, "parkour"); // Updated to join game
            player.closeInventory();
        }
    }
}

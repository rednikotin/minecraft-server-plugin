package com.example.minecraftserver.game;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class GameMenu implements Listener {
    private final Inventory menu;

    public GameMenu() {
        menu = Bukkit.createInventory(null, 9, "Select Game");

        ItemStack pvpItem = new ItemStack(Material.DIAMOND_SWORD);
        ItemMeta pvpMeta = pvpItem.getItemMeta();
        if (pvpMeta != null) {
            pvpMeta.setDisplayName("PVP 1v1");
            pvpItem.setItemMeta(pvpMeta);
        }

        ItemStack parkourItem = new ItemStack(Material.FEATHER);
        ItemMeta parkourMeta = parkourItem.getItemMeta();
        if (parkourMeta != null) {
            parkourMeta.setDisplayName("Parkour");
            parkourItem.setItemMeta(parkourMeta);
        }

        menu.setItem(3, pvpItem);
        menu.setItem(5, parkourItem);
    }

    public void open(Player player) {
        player.openInventory(menu);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (event.getInventory().equals(menu)) {
            event.setCancelled(true);
            Player player = (Player) event.getWhoClicked();
            ItemStack clicked = event.getCurrentItem();
            if (clicked == null || clicked.getType() == Material.AIR) return;

            String name = clicked.getItemMeta() != null ? clicked.getItemMeta().getDisplayName() : "";
            if ("PVP 1v1".equals(name)) {
                PvPGameManager.getInstance().joinQueue(player);
                player.closeInventory();
            } else if ("Parkour".equals(name)) {
                ParkourManager.teleportToStart(player);
                player.closeInventory();
            }
        }
    }
}

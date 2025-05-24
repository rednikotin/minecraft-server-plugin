package com.example.minecraftserver.commands;

import com.example.minecraftserver.Main;
import com.example.minecraftserver.games.Game;
import com.example.minecraftserver.games.GameManager;
import com.example.minecraftserver.menu.MenuManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class GameCommands implements CommandExecutor, TabCompleter {

    private final Main plugin;
    private final MenuManager menuManager;
    private final GameManager gameManager;

    public GameCommands(Main plugin, MenuManager menuManager, GameManager gameManager) {
        this.plugin = plugin;
        this.menuManager = menuManager;
        this.gameManager = gameManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            menuManager.openMainMenu(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "menu":
                menuManager.openMainMenu(player);
                break;
            case "join":
                if (args.length < 2) {
                    player.sendMessage("§cUsage: /game join <gameType>");
                    break;
                }
                String gameType = args[1].toLowerCase();
                // GameManager's getGame method already handles lowercasing the gameType.
                Game gameToJoin = gameManager.getGame(gameType); 
                if (gameToJoin == null) {
                    player.sendMessage("§cInvalid game type. Available: pvp, parkour"); // As per explicit subtask instruction
                    break;
                }
                gameManager.joinGame(player, gameType); // GameManager handles its own messages
                break;
            case "leave":
                gameManager.leaveGame(player); // GameManager handles its own messages
                break;
            default:
                player.sendMessage("§cUnknown command. Usage: /game <menu|join|leave> [gameType]");
                break;
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            StringUtil.copyPartialMatches(args[0], Arrays.asList("menu", "join", "leave"), completions);
        } else if (args.length == 2 && args[0].equalsIgnoreCase("join")) {
            StringUtil.copyPartialMatches(args[1], gameManager.getRegisteredGameNames(), completions);
        }
        Collections.sort(completions);
        return completions;
    }
}

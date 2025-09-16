package net.pallux.prosuggest.commands;

import net.pallux.prosuggest.ProSuggest;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SuggestCommand implements CommandExecutor {

    private final ProSuggest plugin;

    public SuggestCommand(ProSuggest plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getConfigManager().getMessage("player-only"));
            return true;
        }

        Player player = (Player) sender;

        // Check permission
        if (!player.hasPermission("prosuggest.use")) {
            player.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return true;
        }

        // Handle subcommands
        if (args.length > 0) {
            String subcommand = args[0].toLowerCase();

            switch (subcommand) {
                case "reload":
                    if (player.hasPermission("prosuggest.admin")) {
                        plugin.reload();
                        player.sendMessage(plugin.getConfigManager().getMessage("reload-success"));
                    } else {
                        player.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
                    }
                    return true;

                case "help":
                    sendHelpMessage(player);
                    return true;

                default:
                    // Open main GUI for any other argument
                    break;
            }
        }

        // Open main suggestion GUI
        plugin.getGuiManager().openMainGui(player);
        return true;
    }

    private void sendHelpMessage(Player player) {
        player.sendMessage(plugin.getConfigManager().colorize("&6&l=== ProSuggest Help ==="));
        player.sendMessage(plugin.getConfigManager().colorize("&e/suggest &7- Open suggestions GUI"));
        player.sendMessage(plugin.getConfigManager().colorize("&e/suggest help &7- Show this help"));

        if (player.hasPermission("prosuggest.admin")) {
            player.sendMessage(plugin.getConfigManager().colorize("&e/suggest reload &7- Reload configuration"));
            player.sendMessage(plugin.getConfigManager().colorize("&e/suggestadmin &7- Open admin GUI"));
        }
    }
}
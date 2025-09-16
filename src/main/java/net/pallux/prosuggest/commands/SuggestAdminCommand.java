package net.pallux.prosuggest.commands;

import net.pallux.prosuggest.ProSuggest;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SuggestAdminCommand implements CommandExecutor {

    private final ProSuggest plugin;

    public SuggestAdminCommand(ProSuggest plugin) {
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
        if (!player.hasPermission("prosuggest.admin")) {
            player.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return true;
        }

        // Handle subcommands
        if (args.length > 0) {
            String subcommand = args[0].toLowerCase();

            switch (subcommand) {
                case "delete":
                    if (args.length < 2) {
                        player.sendMessage(plugin.getConfigManager().colorize(
                                "&cUsage: /suggestadmin delete <suggestion-id>"));
                        return true;
                    }

                    String deleteId = args[1];
                    if (plugin.getSuggestionManager().deleteSuggestion(deleteId)) {
                        player.sendMessage(plugin.getConfigManager().getMessage("admin.deleted",
                                "%id%", deleteId));
                    } else {
                        player.sendMessage(plugin.getConfigManager().getMessage("delete.not-found",
                                "%id%", deleteId));
                    }
                    return true;

                case "response":
                case "respond":
                    if (args.length < 3) {
                        player.sendMessage(plugin.getConfigManager().colorize(
                                "&cUsage: /suggestadmin response <suggestion-id> <response>"));
                        return true;
                    }

                    String responseId = args[1];
                    StringBuilder response = new StringBuilder();
                    for (int i = 2; i < args.length; i++) {
                        response.append(args[i]).append(" ");
                    }

                    if (plugin.getSuggestionManager().addAdminResponse(responseId, response.toString().trim())) {
                        player.sendMessage(plugin.getConfigManager().getMessage("admin.response-added",
                                "%id%", responseId));
                    } else {
                        player.sendMessage(plugin.getConfigManager().getMessage("delete.not-found",
                                "%id%", responseId));
                    }
                    return true;

                case "list":
                    int total = plugin.getSuggestionManager().getTotalSuggestions();
                    player.sendMessage(plugin.getConfigManager().colorize(
                            "&6Total suggestions: &f" + total));
                    return true;

                case "help":
                    sendAdminHelp(player);
                    return true;

                default:
                    break;
            }
        }

        // Open admin GUI
        plugin.getGuiManager().openAdminGui(player, 1);
        return true;
    }

    private void sendAdminHelp(Player player) {
        player.sendMessage(plugin.getConfigManager().colorize("&6&l=== ProSuggest Admin Help ==="));
        player.sendMessage(plugin.getConfigManager().colorize("&e/suggestadmin &7- Open admin GUI"));
        player.sendMessage(plugin.getConfigManager().colorize("&e/suggestadmin delete <id> &7- Delete suggestion"));
        player.sendMessage(plugin.getConfigManager().colorize("&e/suggestadmin response <id> <text> &7- Add admin response"));
        player.sendMessage(plugin.getConfigManager().colorize("&e/suggestadmin list &7- Show total suggestions"));
        player.sendMessage(plugin.getConfigManager().colorize("&e/suggestadmin help &7- Show this help"));
        player.sendMessage(plugin.getConfigManager().colorize("&7You can also use the GUI for most actions!"));
    }
}
package net.pallux.prosuggest.commands;

import net.pallux.prosuggest.ProSuggest;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class PsAdminCommand implements CommandExecutor {

    private final ProSuggest plugin;

    public PsAdminCommand(ProSuggest plugin) {
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

        // Open admin GUI
        plugin.getGuiManager().openAdminGui(player, 1);
        return true;
    }
}
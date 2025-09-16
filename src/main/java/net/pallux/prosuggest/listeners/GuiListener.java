package net.pallux.prosuggest.listeners;

import net.pallux.prosuggest.ProSuggest;
import net.pallux.prosuggest.managers.GuiManager;
import net.pallux.prosuggest.managers.SuggestionManager;
import net.pallux.prosuggest.models.Suggestion;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class GuiListener implements Listener {

    private final ProSuggest plugin;

    public GuiListener(ProSuggest plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();
        GuiManager.GuiSession session = plugin.getGuiManager().getSession(player.getUniqueId());

        if (session == null) return;

        event.setCancelled(true);

        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem == null || !clickedItem.hasItemMeta()) return;

        ItemMeta meta = clickedItem.getItemMeta();
        if (meta == null || meta.getDisplayName() == null) return;

        String displayName = meta.getDisplayName();
        int slot = event.getSlot();
        ClickType clickType = event.getClick();

        // Handle different GUI types
        switch (session.getGuiType()) {
            case MAIN:
                handleMainGuiClick(player, session, slot, displayName, clickType);
                break;
            case MY_SUGGESTIONS:
                handleMySuggestionsClick(player, session, slot, displayName, clickType);
                break;
            case VIEW_SUGGESTION:
                handleViewSuggestionClick(player, session, slot, displayName, clickType);
                break;
            case ADMIN:
                handleAdminGuiClick(player, session, slot, displayName, clickType);
                break;
        }
    }

    private void handleMainGuiClick(Player player, GuiManager.GuiSession session, int slot,
                                    String displayName, ClickType clickType) {

        // Check for navigation buttons
        if (displayName.contains("Previous Page")) {
            plugin.getGuiManager().openMainGui(player, session.getCurrentPage() - 1, session.getSortType());
            return;
        }

        if (displayName.contains("Next Page")) {
            plugin.getGuiManager().openMainGui(player, session.getCurrentPage() + 1, session.getSortType());
            return;
        }

        // Check for action buttons
        if (displayName.contains("Create New Suggestion")) {
            if (!plugin.getSuggestionManager().canPlayerCreateSuggestion(player.getUniqueId())) {
                int max = plugin.getConfigManager().getMaxSuggestionsPerPlayer();
                player.sendMessage(plugin.getConfigManager().getMessage("create.max-reached",
                        "%max%", String.valueOf(max)));
                return;
            }

            player.closeInventory();
            plugin.getChatListener().startSuggestionCreation(player);
            return;
        }

        if (displayName.contains("My Suggestions")) {
            plugin.getGuiManager().openMysuggestionsGui(player, 1);
            return;
        }

        if (displayName.contains("Delete Suggestion")) {
            plugin.getGuiManager().openMysuggestionsGui(player, 1);
            player.sendMessage(plugin.getConfigManager().colorize(
                    "&eClick on one of your suggestions to delete it!"));
            return;
        }

        // Check for sort button
        if (displayName.contains("Sort:")) {
            SuggestionManager.SortType newSort = session.getSortType() == SuggestionManager.SortType.RECENT
                    ? SuggestionManager.SortType.POPULAR
                    : SuggestionManager.SortType.RECENT;
            plugin.getGuiManager().openMainGui(player, 1, newSort);
            return;
        }

        // Handle suggestion clicks
        if (slot < plugin.getConfigManager().getItemsPerPage()) {
            Suggestion suggestion = session.getSuggestionAtSlot(slot);
            if (suggestion != null) {
                plugin.getGuiManager().openSuggestionView(player, suggestion.getId());
            }
        }
    }

    private void handleMySuggestionsClick(Player player, GuiManager.GuiSession session, int slot,
                                          String displayName, ClickType clickType) {

        // Check for navigation buttons
        if (displayName.contains("Previous Page")) {
            plugin.getGuiManager().openMysuggestionsGui(player, session.getCurrentPage() - 1);
            return;
        }

        if (displayName.contains("Next Page")) {
            plugin.getGuiManager().openMysuggestionsGui(player, session.getCurrentPage() + 1);
            return;
        }

        // Handle suggestion clicks for deletion
        if (slot < plugin.getConfigManager().getItemsPerPage()) {
            Suggestion suggestion = session.getSuggestionAtSlot(slot);
            if (suggestion != null && suggestion.getAuthorUUID().equals(player.getUniqueId())) {

                if (session.isAwaitingConfirmation() &&
                        suggestion.getId().equals(session.getConfirmationTarget())) {

                    // Confirm deletion
                    if (plugin.getSuggestionManager().deleteSuggestion(suggestion.getId())) {
                        player.sendMessage(plugin.getConfigManager().getMessage("delete.success",
                                "%id%", suggestion.getId()));
                        plugin.getGuiManager().openMysuggestionsGui(player, session.getCurrentPage());
                    }

                } else {
                    // First click - request confirmation
                    session.setAwaitingConfirmation(true);
                    session.setConfirmationTarget(suggestion.getId());
                    player.sendMessage(plugin.getConfigManager().getMessage("delete.confirm"));
                }
            }
        }
    }

    private void handleViewSuggestionClick(Player player, GuiManager.GuiSession session, int slot,
                                           String displayName, ClickType clickType) {

        String suggestionId = session.getViewingSuggestionId();
        if (suggestionId == null) return;

        Suggestion suggestion = plugin.getSuggestionManager().getSuggestion(suggestionId);
        if (suggestion == null) return;

        if (displayName.contains("Go Back")) {
            plugin.getGuiManager().openMainGui(player);
            return;
        }

        if (displayName.contains("UPVOTE")) {
            handleVote(player, suggestion, Suggestion.VoteType.UPVOTE);
            plugin.getGuiManager().openSuggestionView(player, suggestionId); // Refresh
            return;
        }

        if (displayName.contains("DOWNVOTE")) {
            handleVote(player, suggestion, Suggestion.VoteType.DOWNVOTE);
            plugin.getGuiManager().openSuggestionView(player, suggestionId); // Refresh
            return;
        }
    }

    private void handleAdminGuiClick(Player player, GuiManager.GuiSession session, int slot,
                                     String displayName, ClickType clickType) {

        // Check for navigation buttons
        if (displayName.contains("Previous Page")) {
            plugin.getGuiManager().openAdminGui(player, session.getCurrentPage() - 1);
            return;
        }

        if (displayName.contains("Next Page")) {
            plugin.getGuiManager().openAdminGui(player, session.getCurrentPage() + 1);
            return;
        }

        // Handle suggestion clicks for admin actions
        if (slot < plugin.getConfigManager().getItemsPerPage()) {
            Suggestion suggestion = session.getSuggestionAtSlot(slot);
            if (suggestion != null) {

                if (clickType == ClickType.LEFT) {
                    // Edit suggestion (start chat listener for editing)
                    player.closeInventory();
                    plugin.getChatListener().startSuggestionEdit(player, suggestion.getId());

                } else if (clickType == ClickType.RIGHT) {
                    // Delete suggestion
                    if (session.isAwaitingConfirmation() &&
                            suggestion.getId().equals(session.getConfirmationTarget())) {

                        // Confirm deletion
                        if (plugin.getSuggestionManager().deleteSuggestion(suggestion.getId())) {
                            player.sendMessage(plugin.getConfigManager().getMessage("admin.deleted",
                                    "%id%", suggestion.getId()));
                            plugin.getGuiManager().openAdminGui(player, session.getCurrentPage());
                        }

                    } else {
                        // First click - request confirmation
                        session.setAwaitingConfirmation(true);
                        session.setConfirmationTarget(suggestion.getId());
                        player.sendMessage(plugin.getConfigManager().getMessage("delete.confirm"));
                    }

                } else if (clickType == ClickType.SHIFT_RIGHT) {
                    // Add admin response
                    player.closeInventory();
                    plugin.getChatListener().startAdminResponse(player, suggestion.getId());
                }
            }
        }
    }

    private void handleVote(Player player, Suggestion suggestion, Suggestion.VoteType voteType) {
        // Check if player can vote
        if (!player.hasPermission("prosuggest.vote")) {
            player.sendMessage(plugin.getConfigManager().getMessage("no-permission"));
            return;
        }

        Suggestion.VoteResult result = plugin.getSuggestionManager().vote(
                suggestion.getId(), player.getUniqueId(), voteType);

        if (result == null) {
            if (suggestion.getAuthorUUID().equals(player.getUniqueId())) {
                player.sendMessage(plugin.getConfigManager().getMessage("vote.own-suggestion"));
            }
            return;
        }

        switch (result) {
            case UPVOTED:
                player.sendMessage(plugin.getConfigManager().getMessage("vote.upvoted",
                        "%id%", suggestion.getId()));
                break;
            case DOWNVOTED:
                player.sendMessage(plugin.getConfigManager().getMessage("vote.downvoted",
                        "%id%", suggestion.getId()));
                break;
            case REMOVED:
                player.sendMessage(plugin.getConfigManager().getMessage("vote.removed",
                        "%id%", suggestion.getId()));
                break;
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;

        Player player = (Player) event.getPlayer();
        GuiManager.GuiSession session = plugin.getGuiManager().getSession(player.getUniqueId());

        if (session != null) {
            // Reset confirmation state when GUI is closed
            session.setAwaitingConfirmation(false);
            session.setConfirmationTarget(null);

            // Don't remove session immediately - keep it for potential reopening
            // Session will be removed when player logs out or starts a new session
        }
    }
}
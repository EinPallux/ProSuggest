package net.pallux.prosuggest.listeners;

import net.pallux.prosuggest.ProSuggest;
import net.pallux.prosuggest.models.Suggestion;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ChatListener implements Listener {

    private final ProSuggest plugin;
    private final Map<UUID, ChatSession> chatSessions;

    public ChatListener(ProSuggest plugin) {
        this.plugin = plugin;
        this.chatSessions = new HashMap<>();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        ChatSession session = chatSessions.get(player.getUniqueId());

        if (session == null) return;

        event.setCancelled(true);
        String message = event.getMessage();

        // Handle cancellation
        if (message.equalsIgnoreCase("cancel") || message.equalsIgnoreCase("exit")) {
            chatSessions.remove(player.getUniqueId());
            player.sendMessage(plugin.getConfigManager().getMessage("create.cancelled"));

            // Reopen GUI after cancellation
            new BukkitRunnable() {
                @Override
                public void run() {
                    plugin.getGuiManager().openMainGui(player);
                }
            }.runTask(plugin);
            return;
        }

        // Handle different session types
        switch (session.getSessionType()) {
            case CREATE_TITLE:
                handleTitleInput(player, session, message);
                break;
            case CREATE_DESCRIPTION:
                handleDescriptionInput(player, session, message);
                break;
            case EDIT_TITLE:
                handleEditTitleInput(player, session, message);
                break;
            case EDIT_DESCRIPTION:
                handleEditDescriptionInput(player, session, message);
                break;
            case ADMIN_RESPONSE:
                handleAdminResponseInput(player, session, message);
                break;
        }
    }

    private void handleTitleInput(Player player, ChatSession session, String title) {
        // Validate title length
        int maxLength = plugin.getConfigManager().getMaxTitleLength();
        if (title.length() > maxLength) {
            player.sendMessage(plugin.getConfigManager().getMessage("create.title-too-long",
                    "%max%", String.valueOf(maxLength)));
            return;
        }

        // Store title and move to description
        session.setTitle(title);
        session.setSessionType(ChatSessionType.CREATE_DESCRIPTION);

        player.sendMessage(plugin.getConfigManager().getMessage("create.title-received",
                "%title%", title));
        player.sendMessage(plugin.getConfigManager().getMessage("create.description-prompt"));

        // Reset timeout
        session.resetTimeout();
    }

    private void handleDescriptionInput(Player player, ChatSession session, String description) {
        // Validate description length
        int maxLength = plugin.getConfigManager().getMaxDescriptionLength();
        if (description.length() > maxLength) {
            player.sendMessage(plugin.getConfigManager().getMessage("create.description-too-long",
                    "%max%", String.valueOf(maxLength)));
            return;
        }

        // Create the suggestion
        String suggestionId = plugin.getSuggestionManager().createSuggestion(
                session.getTitle(), description, player.getUniqueId(), player.getName());

        chatSessions.remove(player.getUniqueId());

        if (suggestionId != null) {
            player.sendMessage(plugin.getConfigManager().getMessage("create.success",
                    "%id%", suggestionId));
        } else {
            int max = plugin.getConfigManager().getMaxSuggestionsPerPlayer();
            player.sendMessage(plugin.getConfigManager().getMessage("create.max-reached",
                    "%max%", String.valueOf(max)));
        }

        // Reopen main GUI
        new BukkitRunnable() {
            @Override
            public void run() {
                plugin.getGuiManager().openMainGui(player);
            }
        }.runTask(plugin);
    }

    private void handleEditTitleInput(Player player, ChatSession session, String newTitle) {
        // Validate title length
        int maxLength = plugin.getConfigManager().getMaxTitleLength();
        if (newTitle.length() > maxLength) {
            player.sendMessage(plugin.getConfigManager().getMessage("create.title-too-long",
                    "%max%", String.valueOf(maxLength)));
            return;
        }

        session.setTitle(newTitle);
        session.setSessionType(ChatSessionType.EDIT_DESCRIPTION);

        player.sendMessage(plugin.getConfigManager().getMessage("create.title-received",
                "%title%", newTitle));
        player.sendMessage(plugin.getConfigManager().getMessage("create.description-prompt"));

        session.resetTimeout();
    }

    private void handleEditDescriptionInput(Player player, ChatSession session, String newDescription) {
        // Validate description length
        int maxLength = plugin.getConfigManager().getMaxDescriptionLength();
        if (newDescription.length() > maxLength) {
            player.sendMessage(plugin.getConfigManager().getMessage("create.description-too-long",
                    "%max%", String.valueOf(maxLength)));
            return;
        }

        // Update the suggestion
        boolean success = plugin.getSuggestionManager().editSuggestion(
                session.getTargetId(), session.getTitle(), newDescription);

        chatSessions.remove(player.getUniqueId());

        if (success) {
            player.sendMessage(plugin.getConfigManager().getMessage("admin.edited",
                    "%id%", session.getTargetId()));
        } else {
            player.sendMessage(plugin.getConfigManager().getMessage("delete.not-found",
                    "%id%", session.getTargetId()));
        }

        // Reopen admin GUI
        new BukkitRunnable() {
            @Override
            public void run() {
                plugin.getGuiManager().openAdminGui(player, 1);
            }
        }.runTask(plugin);
    }

    private void handleAdminResponseInput(Player player, ChatSession session, String response) {
        boolean success = plugin.getSuggestionManager().addAdminResponse(
                session.getTargetId(), response);

        chatSessions.remove(player.getUniqueId());

        if (success) {
            player.sendMessage(plugin.getConfigManager().getMessage("admin.response-added",
                    "%id%", session.getTargetId()));
        } else {
            player.sendMessage(plugin.getConfigManager().getMessage("delete.not-found",
                    "%id%", session.getTargetId()));
        }

        // Reopen admin GUI
        new BukkitRunnable() {
            @Override
            public void run() {
                plugin.getGuiManager().openAdminGui(player, 1);
            }
        }.runTask(plugin);
    }

    // Public methods to start different chat sessions
    public void startSuggestionCreation(Player player) {
        ChatSession session = new ChatSession(ChatSessionType.CREATE_TITLE, plugin);
        chatSessions.put(player.getUniqueId(), session);

        player.sendMessage(plugin.getConfigManager().getMessage("create.title-prompt"));
        player.sendMessage(plugin.getConfigManager().colorize("&7Type 'cancel' to abort."));
    }

    public void startSuggestionEdit(Player player, String suggestionId) {
        Suggestion suggestion = plugin.getSuggestionManager().getSuggestion(suggestionId);
        if (suggestion == null) {
            player.sendMessage(plugin.getConfigManager().getMessage("delete.not-found",
                    "%id%", suggestionId));
            return;
        }

        ChatSession session = new ChatSession(ChatSessionType.EDIT_TITLE, plugin);
        session.setTargetId(suggestionId);
        chatSessions.put(player.getUniqueId(), session);

        player.sendMessage(plugin.getConfigManager().colorize("&eEditing suggestion: &f" + suggestionId));
        player.sendMessage(plugin.getConfigManager().colorize("&7Current title: &f" + suggestion.getTitle()));
        player.sendMessage(plugin.getConfigManager().getMessage("create.title-prompt"));
        player.sendMessage(plugin.getConfigManager().colorize("&7Type 'cancel' to abort."));
    }

    public void startAdminResponse(Player player, String suggestionId) {
        Suggestion suggestion = plugin.getSuggestionManager().getSuggestion(suggestionId);
        if (suggestion == null) {
            player.sendMessage(plugin.getConfigManager().getMessage("delete.not-found",
                    "%id%", suggestionId));
            return;
        }

        ChatSession session = new ChatSession(ChatSessionType.ADMIN_RESPONSE, plugin);
        session.setTargetId(suggestionId);
        chatSessions.put(player.getUniqueId(), session);

        player.sendMessage(plugin.getConfigManager().colorize("&eAdding response to: &f" + suggestionId));
        player.sendMessage(plugin.getConfigManager().colorize("&7Title: &f" + suggestion.getTitle()));
        player.sendMessage(plugin.getConfigManager().colorize("&eEnter your admin response:"));
        player.sendMessage(plugin.getConfigManager().colorize("&7Type 'cancel' to abort."));
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Clean up any existing sessions for this player
        chatSessions.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Clean up sessions and GUI sessions when player leaves
        chatSessions.remove(event.getPlayer().getUniqueId());
        plugin.getGuiManager().removeSession(event.getPlayer().getUniqueId());
    }

    // Inner classes
    private static class ChatSession {
        private ChatSessionType sessionType;
        private String title;
        private String targetId; // For editing existing suggestions
        private BukkitRunnable timeoutTask;

        public ChatSession(ChatSessionType sessionType, ProSuggest plugin) {
            this.sessionType = sessionType;
            startTimeout(plugin);
        }

        private void startTimeout(ProSuggest plugin) {
            timeoutTask = new BukkitRunnable() {
                @Override
                public void run() {
                    // Session timeout handling
                    plugin.getLogger().info("Chat session timed out for a player");
                }
            };
            timeoutTask.runTaskLater(plugin, 20 * 60 * 5); // 5 minute timeout
        }

        public void resetTimeout() {
            if (timeoutTask != null && !timeoutTask.isCancelled()) {
                timeoutTask.cancel();
            }
        }

        // Getters and setters
        public ChatSessionType getSessionType() { return sessionType; }
        public void setSessionType(ChatSessionType sessionType) { this.sessionType = sessionType; }

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        public String getTargetId() { return targetId; }
        public void setTargetId(String targetId) { this.targetId = targetId; }
    }

    private enum ChatSessionType {
        CREATE_TITLE, CREATE_DESCRIPTION, EDIT_TITLE, EDIT_DESCRIPTION, ADMIN_RESPONSE
    }
}
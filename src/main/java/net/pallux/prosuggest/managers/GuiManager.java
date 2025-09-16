package net.pallux.prosuggest.managers;

import net.pallux.prosuggest.ProSuggest;
import net.pallux.prosuggest.models.Suggestion;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class GuiManager {

    private final ProSuggest plugin;
    private final Map<UUID, GuiSession> sessions;

    public GuiManager(ProSuggest plugin) {
        this.plugin = plugin;
        this.sessions = new HashMap<>();
    }

    public void openMainGui(Player player) {
        openMainGui(player, 1, SuggestionManager.SortType.valueOf(plugin.getConfigManager().getDefaultSort()));
    }

    public void openMainGui(Player player, int page, SuggestionManager.SortType sortType) {
        List<Suggestion> suggestions = plugin.getSuggestionManager().getSortedSuggestions(sortType);

        int itemsPerPage = plugin.getConfigManager().getItemsPerPage();
        int totalPages = Math.max(1, (int) Math.ceil((double) suggestions.size() / itemsPerPage));

        if (page > totalPages) page = totalPages;
        if (page < 1) page = 1;

        int guiSize = plugin.getConfigManager().getGuiSize();
        String title = plugin.getConfigManager().getGuiTitle("main")
                .replace("%page%", String.valueOf(page))
                .replace("%total%", String.valueOf(totalPages));

        Inventory gui = Bukkit.createInventory(null, guiSize, title);

        // Add suggestions
        int startIndex = (page - 1) * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, suggestions.size());

        for (int i = startIndex; i < endIndex; i++) {
            Suggestion suggestion = suggestions.get(i);
            ItemStack item = createSuggestionItem(suggestion, player);
            gui.addItem(item);
        }

        // Add navigation and action buttons
        addNavigationButtons(gui, page, totalPages, sortType, GuiType.MAIN);

        // Create session
        GuiSession session = new GuiSession(GuiType.MAIN, page, sortType, suggestions);
        sessions.put(player.getUniqueId(), session);

        player.openInventory(gui);
    }

    public void openMysuggestionsGui(Player player, int page) {
        List<Suggestion> suggestions = plugin.getSuggestionManager()
                .getSuggestionsByPlayer(player.getUniqueId());

        int itemsPerPage = plugin.getConfigManager().getItemsPerPage();
        int totalPages = Math.max(1, (int) Math.ceil((double) suggestions.size() / itemsPerPage));

        if (page > totalPages) page = totalPages;
        if (page < 1) page = 1;

        int guiSize = plugin.getConfigManager().getGuiSize();
        String title = plugin.getConfigManager().getGuiTitle("my-suggestions")
                .replace("%page%", String.valueOf(page));

        Inventory gui = Bukkit.createInventory(null, guiSize, title);

        // Add suggestions
        int startIndex = (page - 1) * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, suggestions.size());

        for (int i = startIndex; i < endIndex; i++) {
            Suggestion suggestion = suggestions.get(i);
            ItemStack item = createSuggestionItem(suggestion, player);
            gui.addItem(item);
        }

        // Add navigation buttons
        addNavigationButtons(gui, page, totalPages, SuggestionManager.SortType.RECENT, GuiType.MY_SUGGESTIONS);

        // Create session
        GuiSession session = new GuiSession(GuiType.MY_SUGGESTIONS, page, SuggestionManager.SortType.RECENT, suggestions);
        sessions.put(player.getUniqueId(), session);

        player.openInventory(gui);
    }

    public void openSuggestionView(Player player, String suggestionId) {
        Suggestion suggestion = plugin.getSuggestionManager().getSuggestion(suggestionId);
        if (suggestion == null) return;

        String title = plugin.getConfigManager().getGuiTitle("view")
                .replace("%id%", suggestionId);

        Inventory gui = Bukkit.createInventory(null, 27, title);

        // Suggestion display item
        ItemStack suggestionItem = createDetailedSuggestionItem(suggestion, player);
        gui.setItem(13, suggestionItem);

        // Voting buttons
        ItemStack upvoteItem = createVoteItem("upvote", suggestion);
        ItemStack downvoteItem = createVoteItem("downvote", suggestion);

        gui.setItem(11, upvoteItem);
        gui.setItem(15, downvoteItem);

        // Back button
        ItemStack backItem = createBackButton();
        gui.setItem(18, backItem);

        // Fill empty slots
        fillEmptySlots(gui);

        // Create session
        GuiSession session = new GuiSession(GuiType.VIEW_SUGGESTION, 1, SuggestionManager.SortType.RECENT,
                Collections.singletonList(suggestion));
        session.setViewingSuggestionId(suggestionId);
        sessions.put(player.getUniqueId(), session);

        player.openInventory(gui);
    }

    public void openAdminGui(Player player, int page) {
        List<Suggestion> suggestions = plugin.getSuggestionManager().getAllSuggestions();

        int itemsPerPage = plugin.getConfigManager().getItemsPerPage();
        int totalPages = Math.max(1, (int) Math.ceil((double) suggestions.size() / itemsPerPage));

        if (page > totalPages) page = totalPages;
        if (page < 1) page = 1;

        int guiSize = plugin.getConfigManager().getGuiSize();
        String title = plugin.getConfigManager().getGuiTitle("admin")
                .replace("%page%", String.valueOf(page));

        Inventory gui = Bukkit.createInventory(null, guiSize, title);

        // Add suggestions with admin context
        int startIndex = (page - 1) * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, suggestions.size());

        for (int i = startIndex; i < endIndex; i++) {
            Suggestion suggestion = suggestions.get(i);
            ItemStack item = createAdminSuggestionItem(suggestion, player);
            gui.addItem(item);
        }

        // Add navigation buttons
        addNavigationButtons(gui, page, totalPages, SuggestionManager.SortType.RECENT, GuiType.ADMIN);

        // Create session
        GuiSession session = new GuiSession(GuiType.ADMIN, page, SuggestionManager.SortType.RECENT, suggestions);
        sessions.put(player.getUniqueId(), session);

        player.openInventory(gui);
    }

    private ItemStack createSuggestionItem(Suggestion suggestion, Player viewer) {
        Material material = Material.valueOf(plugin.getConfigManager().getItemMaterial("suggestion"));
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            // Title
            String name = plugin.getConfigManager().getItemName("suggestion")
                    .replace("%title%", suggestion.getTitle())
                    .replace("%id%", suggestion.getId());
            meta.setDisplayName(name);

            // Lore
            List<String> lore = new ArrayList<>();
            String[] loreLines = plugin.getConfigManager().getItemLore("suggestion").split("\n");

            for (String line : loreLines) {
                String processedLine = line
                        .replace("%author%", suggestion.getAuthorName())
                        .replace("%id%", suggestion.getId())
                        .replace("%date%", suggestion.getFormattedDate())
                        .replace("%upvotes%", String.valueOf(suggestion.getUpvoteCount()))
                        .replace("%downvotes%", String.valueOf(suggestion.getDownvoteCount()))
                        .replace("%description%", suggestion.getDescription());

                lore.add(plugin.getConfigManager().colorize(processedLine));
            }

            // Add vote status if player has voted
            Suggestion.VoteType voteType = suggestion.getVoteType(viewer.getUniqueId());
            if (voteType != null) {
                if (voteType == Suggestion.VoteType.UPVOTE) {
                    lore.add(plugin.getConfigManager().getMessage("gui.voted-up"));
                } else {
                    lore.add(plugin.getConfigManager().getMessage("gui.voted-down"));
                }
            }

            // Mark own suggestions
            if (suggestion.getAuthorUUID().equals(viewer.getUniqueId())) {
                lore.add(plugin.getConfigManager().getMessage("gui.own-suggestion"));
            }

            // Add admin response if exists
            if (suggestion.getAdminResponse() != null) {
                lore.add("");
                lore.add(plugin.getConfigManager().getMessage("gui.admin-response",
                        "%response%", suggestion.getAdminResponse()));
            }

            meta.setLore(lore);
            item.setItemMeta(meta);
        }

        return item;
    }

    private ItemStack createDetailedSuggestionItem(Suggestion suggestion, Player viewer) {
        Material material = Material.valueOf(plugin.getConfigManager().getItemMaterial("suggestion"));
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            String name = plugin.getConfigManager().colorize("&e&l" + suggestion.getTitle());
            meta.setDisplayName(name);

            List<String> lore = new ArrayList<>();
            lore.add(plugin.getConfigManager().colorize("&7By: &f" + suggestion.getAuthorName()));
            lore.add(plugin.getConfigManager().colorize("&7ID: &f" + suggestion.getId()));
            lore.add(plugin.getConfigManager().colorize("&7Created: &f" + suggestion.getFormattedDate()));
            lore.add("");
            lore.add(plugin.getConfigManager().colorize("&a▲ &f" + suggestion.getUpvoteCount() +
                    " &c▼ &f" + suggestion.getDownvoteCount()));
            lore.add("");
            lore.add(plugin.getConfigManager().colorize("&7Description:"));

            // Wrap description
            List<String> wrappedDesc = suggestion.getWrappedDescription(40);
            for (String line : wrappedDesc) {
                lore.add(plugin.getConfigManager().colorize("&f" + line));
            }

            if (suggestion.getAdminResponse() != null) {
                lore.add("");
                lore.add(plugin.getConfigManager().getMessage("gui.admin-response",
                        "%response%", suggestion.getAdminResponse()));
            }

            meta.setLore(lore);
            item.setItemMeta(meta);
        }

        return item;
    }

    private ItemStack createAdminSuggestionItem(Suggestion suggestion, Player viewer) {
        ItemStack item = createSuggestionItem(suggestion, viewer);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            List<String> lore = meta.getLore();
            if (lore == null) lore = new ArrayList<>();

            lore.add("");
            lore.add(plugin.getConfigManager().colorize("&c&lADMIN ACTIONS:"));
            lore.add(plugin.getConfigManager().colorize("&7Left Click: Edit"));
            lore.add(plugin.getConfigManager().colorize("&7Right Click: Delete"));
            lore.add(plugin.getConfigManager().colorize("&7Shift+Right: Add Response"));

            meta.setLore(lore);
            item.setItemMeta(meta);
        }

        return item;
    }

    private ItemStack createVoteItem(String voteType, Suggestion suggestion) {
        Material material = Material.valueOf(plugin.getConfigManager().getItemMaterial(voteType));
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            String name = plugin.getConfigManager().getItemName(voteType);
            if (voteType.equals("upvote")) {
                name = name.replace("%upvotes%", String.valueOf(suggestion.getUpvoteCount()));
            } else {
                name = name.replace("%downvotes%", String.valueOf(suggestion.getDownvoteCount()));
            }
            meta.setDisplayName(name);

            List<String> lore = Arrays.asList(plugin.getConfigManager().getItemLore(voteType).split("\n"));
            List<String> processedLore = new ArrayList<>();
            for (String line : lore) {
                if (voteType.equals("upvote")) {
                    line = line.replace("%upvotes%", String.valueOf(suggestion.getUpvoteCount()));
                } else {
                    line = line.replace("%downvotes%", String.valueOf(suggestion.getDownvoteCount()));
                }
                processedLore.add(plugin.getConfigManager().colorize(line));
            }
            meta.setLore(processedLore);

            item.setItemMeta(meta);
        }

        return item;
    }

    private void addNavigationButtons(Inventory gui, int currentPage, int totalPages,
                                      SuggestionManager.SortType sortType, GuiType guiType) {
        int guiSize = gui.getSize();

        // Previous page button
        if (currentPage > 1) {
            ItemStack prevItem = createNavigationItem("previous-page", currentPage - 1);
            gui.setItem(plugin.getConfigManager().getItemSlot("previous-page"), prevItem);
        }

        // Next page button
        if (currentPage < totalPages) {
            ItemStack nextItem = createNavigationItem("next-page", currentPage + 1);
            gui.setItem(plugin.getConfigManager().getItemSlot("next-page"), nextItem);
        }

        // Action buttons (only for main GUI)
        if (guiType == GuiType.MAIN) {
            // Create suggestion button
            ItemStack createItem = createActionItem("create-suggestion");
            gui.setItem(plugin.getConfigManager().getItemSlot("create-suggestion"), createItem);

            // My suggestions button
            ItemStack myItem = createActionItem("my-suggestions");
            gui.setItem(plugin.getConfigManager().getItemSlot("my-suggestions"), myItem);

            // Delete suggestion button
            ItemStack deleteItem = createActionItem("delete-suggestion");
            gui.setItem(plugin.getConfigManager().getItemSlot("delete-suggestion"), deleteItem);

            // Sort button
            String sortItemType = sortType == SuggestionManager.SortType.RECENT ? "sort-recent" : "sort-popular";
            ItemStack sortItem = createActionItem(sortItemType);
            gui.setItem(plugin.getConfigManager().getItemSlot("sort-recent"), sortItem);
        }

        // Fill empty slots in bottom row
        fillBottomRow(gui);
    }

    private ItemStack createNavigationItem(String itemType, int targetPage) {
        Material material = Material.valueOf(plugin.getConfigManager().getItemMaterial(itemType));
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            String name = plugin.getConfigManager().getItemName(itemType)
                    .replace("%page%", String.valueOf(targetPage));
            meta.setDisplayName(name);

            List<String> lore = Arrays.asList(plugin.getConfigManager().getItemLore(itemType).split("\n"));
            List<String> processedLore = new ArrayList<>();
            for (String line : lore) {
                processedLore.add(plugin.getConfigManager().colorize(
                        line.replace("%page%", String.valueOf(targetPage))));
            }
            meta.setLore(processedLore);

            item.setItemMeta(meta);
        }

        return item;
    }

    private ItemStack createActionItem(String itemType) {
        Material material = Material.valueOf(plugin.getConfigManager().getItemMaterial(itemType));
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(plugin.getConfigManager().getItemName(itemType));

            List<String> lore = Arrays.asList(plugin.getConfigManager().getItemLore(itemType).split("\n"));
            List<String> processedLore = new ArrayList<>();
            for (String line : lore) {
                processedLore.add(plugin.getConfigManager().colorize(line));
            }
            meta.setLore(processedLore);

            item.setItemMeta(meta);
        }

        return item;
    }

    private ItemStack createBackButton() {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(plugin.getConfigManager().colorize("&c&l← Go Back"));
            meta.setLore(Arrays.asList(plugin.getConfigManager().colorize("&7Return to previous menu")));
            item.setItemMeta(meta);
        }

        return item;
    }

    private void fillEmptySlots(Inventory gui) {
        Material fillerMaterial = Material.valueOf(plugin.getConfigManager().getItemMaterial("filler"));
        ItemStack filler = new ItemStack(fillerMaterial);
        ItemMeta meta = filler.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(plugin.getConfigManager().getItemName("filler"));
            filler.setItemMeta(meta);
        }

        for (int i = 0; i < gui.getSize(); i++) {
            if (gui.getItem(i) == null) {
                gui.setItem(i, filler);
            }
        }
    }

    private void fillBottomRow(Inventory gui) {
        Material fillerMaterial = Material.valueOf(plugin.getConfigManager().getItemMaterial("filler"));
        ItemStack filler = new ItemStack(fillerMaterial);
        ItemMeta meta = filler.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(plugin.getConfigManager().getItemName("filler"));
            filler.setItemMeta(meta);
        }

        int bottomRowStart = gui.getSize() - 9;
        for (int i = bottomRowStart; i < gui.getSize(); i++) {
            if (gui.getItem(i) == null) {
                gui.setItem(i, filler);
            }
        }
    }

    public GuiSession getSession(UUID playerUUID) {
        return sessions.get(playerUUID);
    }

    public void removeSession(UUID playerUUID) {
        sessions.remove(playerUUID);
    }

    // Inner classes
    public static class GuiSession {
        private GuiType guiType;
        private int currentPage;
        private SuggestionManager.SortType sortType;
        private List<Suggestion> currentSuggestions;
        private String viewingSuggestionId;
        private boolean awaitingConfirmation;
        private String confirmationTarget;

        public GuiSession(GuiType guiType, int currentPage, SuggestionManager.SortType sortType,
                          List<Suggestion> currentSuggestions) {
            this.guiType = guiType;
            this.currentPage = currentPage;
            this.sortType = sortType;
            this.currentSuggestions = new ArrayList<>(currentSuggestions);
            this.awaitingConfirmation = false;
        }

        // Getters and setters
        public GuiType getGuiType() { return guiType; }
        public void setGuiType(GuiType guiType) { this.guiType = guiType; }

        public int getCurrentPage() { return currentPage; }
        public void setCurrentPage(int currentPage) { this.currentPage = currentPage; }

        public SuggestionManager.SortType getSortType() { return sortType; }
        public void setSortType(SuggestionManager.SortType sortType) { this.sortType = sortType; }

        public List<Suggestion> getCurrentSuggestions() { return currentSuggestions; }
        public void setCurrentSuggestions(List<Suggestion> currentSuggestions) {
            this.currentSuggestions = new ArrayList<>(currentSuggestions);
        }

        public String getViewingSuggestionId() { return viewingSuggestionId; }
        public void setViewingSuggestionId(String viewingSuggestionId) { this.viewingSuggestionId = viewingSuggestionId; }

        public boolean isAwaitingConfirmation() { return awaitingConfirmation; }
        public void setAwaitingConfirmation(boolean awaitingConfirmation) { this.awaitingConfirmation = awaitingConfirmation; }

        public String getConfirmationTarget() { return confirmationTarget; }
        public void setConfirmationTarget(String confirmationTarget) { this.confirmationTarget = confirmationTarget; }

        public Suggestion getSuggestionAtSlot(int slot) {
            if (slot < 0 || slot >= currentSuggestions.size()) return null;

            int itemsPerPage = 45; // This should be configurable
            int startIndex = (currentPage - 1) * itemsPerPage;
            int actualIndex = startIndex + slot;

            if (actualIndex < currentSuggestions.size()) {
                return currentSuggestions.get(actualIndex);
            }

            return null;
        }
    }

    public enum GuiType {
        MAIN, MY_SUGGESTIONS, VIEW_SUGGESTION, ADMIN
    }
}
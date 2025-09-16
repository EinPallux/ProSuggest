package net.pallux.prosuggest.managers;

import net.pallux.prosuggest.ProSuggest;
import net.pallux.prosuggest.models.Suggestion;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.serialization.ConfigurationSerialization;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class SuggestionManager {

    private final ProSuggest plugin;
    private final Map<String, Suggestion> suggestions;
    private final AtomicInteger idCounter;
    private File suggestionsFile;

    public SuggestionManager(ProSuggest plugin) {
        this.plugin = plugin;
        this.suggestions = new LinkedHashMap<>();
        this.idCounter = new AtomicInteger(1);

        // Register serialization
        ConfigurationSerialization.registerClass(Suggestion.class);
    }

    public void loadSuggestions() {
        suggestionsFile = new File(plugin.getDataFolder(), plugin.getConfigManager().getStorageFileName());

        if (!suggestionsFile.exists()) {
            try {
                suggestionsFile.getParentFile().mkdirs();
                suggestionsFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().severe("Could not create suggestions file!");
                e.printStackTrace();
                return;
            }
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(suggestionsFile);

        // Load suggestions
        if (config.contains("suggestions")) {
            for (String key : config.getConfigurationSection("suggestions").getKeys(false)) {
                try {
                    Suggestion suggestion = (Suggestion) config.get("suggestions." + key);
                    if (suggestion != null) {
                        suggestions.put(key, suggestion);
                        // Update counter to avoid ID conflicts
                        try {
                            int id = Integer.parseInt(key.substring(2)); // Remove "s-" prefix
                            if (id >= idCounter.get()) {
                                idCounter.set(id + 1);
                            }
                        } catch (NumberFormatException e) {
                            // Ignore malformed IDs
                        }
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Could not load suggestion with key: " + key);
                    e.printStackTrace();
                }
            }
        }

        plugin.getLogger().info("Loaded " + suggestions.size() + " suggestions");
    }

    public void saveSuggestions() {
        if (suggestionsFile == null) return;

        FileConfiguration config = new YamlConfiguration();

        // Save all suggestions
        for (Map.Entry<String, Suggestion> entry : suggestions.entrySet()) {
            config.set("suggestions." + entry.getKey(), entry.getValue());
        }

        try {
            config.save(suggestionsFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save suggestions!");
            e.printStackTrace();
        }
    }

    public String createSuggestion(String title, String description, UUID authorUUID, String authorName) {
        // Check if player has reached maximum suggestions
        int maxSuggestions = plugin.getConfigManager().getMaxSuggestionsPerPlayer();
        if (maxSuggestions > 0) {
            long playerSuggestions = suggestions.values().stream()
                    .filter(s -> s.getAuthorUUID().equals(authorUUID))
                    .count();

            if (playerSuggestions >= maxSuggestions) {
                return null; // Max reached
            }
        }

        String id = generateId();
        Suggestion suggestion = new Suggestion(id, title, description, authorUUID, authorName);
        suggestions.put(id, suggestion);
        saveSuggestions();

        return id;
    }

    public boolean deleteSuggestion(String id) {
        if (suggestions.containsKey(id)) {
            suggestions.remove(id);
            saveSuggestions();
            return true;
        }
        return false;
    }

    public Suggestion getSuggestion(String id) {
        return suggestions.get(id);
    }

    public List<Suggestion> getAllSuggestions() {
        return new ArrayList<>(suggestions.values());
    }

    public List<Suggestion> getSuggestionsByPlayer(UUID playerUUID) {
        return suggestions.values().stream()
                .filter(s -> s.getAuthorUUID().equals(playerUUID))
                .collect(Collectors.toList());
    }

    public List<Suggestion> getSortedSuggestions(SortType sortType) {
        List<Suggestion> sorted = new ArrayList<>(suggestions.values());

        switch (sortType) {
            case RECENT:
                sorted.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
                break;
            case POPULAR:
                sorted.sort((a, b) -> Integer.compare(b.getScore(), a.getScore()));
                break;
        }

        return sorted;
    }

    public List<Suggestion> getSortedSuggestionsByPlayer(UUID playerUUID, SortType sortType) {
        List<Suggestion> playerSuggestions = getSuggestionsByPlayer(playerUUID);

        switch (sortType) {
            case RECENT:
                playerSuggestions.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
                break;
            case POPULAR:
                playerSuggestions.sort((a, b) -> Integer.compare(b.getScore(), a.getScore()));
                break;
        }

        return playerSuggestions;
    }

    public Suggestion.VoteResult vote(String suggestionId, UUID playerUUID, Suggestion.VoteType voteType) {
        Suggestion suggestion = suggestions.get(suggestionId);
        if (suggestion == null) {
            return null;
        }

        // Check if player is trying to vote on their own suggestion
        if (!plugin.getConfigManager().isAllowSelfVote() &&
                suggestion.getAuthorUUID().equals(playerUUID)) {
            return null;
        }

        Suggestion.VoteResult result = suggestion.vote(playerUUID, voteType);
        saveSuggestions();
        return result;
    }

    public boolean editSuggestion(String id, String newTitle, String newDescription) {
        Suggestion suggestion = suggestions.get(id);
        if (suggestion != null) {
            suggestion.setTitle(newTitle);
            suggestion.setDescription(newDescription);
            saveSuggestions();
            return true;
        }
        return false;
    }

    public boolean addAdminResponse(String id, String response) {
        Suggestion suggestion = suggestions.get(id);
        if (suggestion != null) {
            suggestion.setAdminResponse(response);
            saveSuggestions();
            return true;
        }
        return false;
    }

    public int getPlayerSuggestionCount(UUID playerUUID) {
        return (int) suggestions.values().stream()
                .filter(s -> s.getAuthorUUID().equals(playerUUID))
                .count();
    }

    public boolean canPlayerCreateSuggestion(UUID playerUUID) {
        int maxSuggestions = plugin.getConfigManager().getMaxSuggestionsPerPlayer();
        if (maxSuggestions <= 0) return true;

        return getPlayerSuggestionCount(playerUUID) < maxSuggestions;
    }

    private String generateId() {
        String id;
        do {
            id = "s-" + String.format("%03d", idCounter.getAndIncrement());
        } while (suggestions.containsKey(id));

        return id;
    }

    public int getTotalSuggestions() {
        return suggestions.size();
    }

    public enum SortType {
        RECENT, POPULAR
    }
}
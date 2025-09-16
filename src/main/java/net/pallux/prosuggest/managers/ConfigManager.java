package net.pallux.prosuggest.managers;

import net.pallux.prosuggest.ProSuggest;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

public class ConfigManager {

    private final ProSuggest plugin;
    private FileConfiguration config;
    private FileConfiguration messages;

    public ConfigManager(ProSuggest plugin) {
        this.plugin = plugin;
    }

    public void loadConfigs() {
        loadConfig();
        loadMessages();
    }

    private void loadConfig() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        config = plugin.getConfig();

        // Save default config if it doesn't exist
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            plugin.saveResource("config.yml", false);
        }
    }

    private void loadMessages() {
        File messagesFile = new File(plugin.getDataFolder(), "messages.yml");

        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }

        messages = YamlConfiguration.loadConfiguration(messagesFile);

        // Load defaults from jar
        try (Reader defConfigStream = new InputStreamReader(
                plugin.getResource("messages.yml"), StandardCharsets.UTF_8)) {
            if (defConfigStream != null) {
                YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(defConfigStream);
                messages.setDefaults(defConfig);
            }
        } catch (IOException e) {
            plugin.getLogger().warning("Could not load default messages.yml from jar");
        }
    }

    public void saveMessages() {
        File messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        try {
            messages.save(messagesFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save messages.yml!");
            e.printStackTrace();
        }
    }

    // Config getters
    public int getGuiSize() {
        return config.getInt("gui.main-size", 54);
    }

    public int getItemsPerPage() {
        return config.getInt("gui.items-per-page", 45);
    }

    public String getGuiTitle(String type) {
        return colorize(config.getString("gui.titles." + type, "&6&lSuggestions"));
    }

    public int getMaxTitleLength() {
        return config.getInt("suggestions.max-title-length", 32);
    }

    public int getMaxDescriptionLength() {
        return config.getInt("suggestions.max-description-length", 200);
    }

    public String getDefaultSort() {
        return config.getString("suggestions.default-sort", "RECENT");
    }

    public boolean isAllowSelfVote() {
        return config.getBoolean("suggestions.allow-self-vote", false);
    }

    public int getMaxSuggestionsPerPlayer() {
        return config.getInt("suggestions.max-per-player", 5);
    }

    public String getStorageFileName() {
        return config.getString("storage.file-name", "suggestions.yml");
    }

    // GUI Item configuration getters
    public String getItemMaterial(String itemPath) {
        return config.getString("gui.items." + itemPath + ".material", "PAPER");
    }

    public String getItemName(String itemPath) {
        return colorize(config.getString("gui.items." + itemPath + ".name", ""));
    }

    public String getItemLore(String itemPath) {
        return String.join("\n", config.getStringList("gui.items." + itemPath + ".lore"));
    }

    public int getItemSlot(String itemPath) {
        return config.getInt("gui.items." + itemPath + ".slot", 0);
    }

    // Message getters
    public String getMessage(String path) {
        String message = messages.getString(path, "&cMessage not found: " + path);
        // Replace prefix placeholder before colorizing
        message = message.replace("%prefix%", getPrefix());
        return colorize(message);
    }

    public String getMessage(String path, String... replacements) {
        String message = messages.getString(path, "&cMessage not found: " + path);

        for (int i = 0; i < replacements.length; i += 2) {
            if (i + 1 < replacements.length) {
                message = message.replace(replacements[i], replacements[i + 1]);
            }
        }

        // Replace prefix placeholder after other replacements
        message = message.replace("%prefix%", getPrefix());

        return colorize(message);
    }

    public String getPrefix() {
        return colorize(messages.getString("prefix", "&6[ProSuggest]&7 "));
    }

    // Utility methods
    public String colorize(String text) {
        if (text == null) return "";
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    public String stripColor(String text) {
        if (text == null) return "";
        return ChatColor.stripColor(colorize(text));
    }

    // Getters for raw configurations
    public FileConfiguration getConfig() {
        return config;
    }

    public FileConfiguration getMessages() {
        return messages;
    }
}
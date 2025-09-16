package net.pallux.prosuggest;

import net.pallux.prosuggest.commands.PsAdminCommand;
import net.pallux.prosuggest.commands.SuggestCommand;
import net.pallux.prosuggest.listeners.ChatListener;
import net.pallux.prosuggest.listeners.GuiListener;
import net.pallux.prosuggest.managers.ConfigManager;
import net.pallux.prosuggest.managers.SuggestionManager;
import net.pallux.prosuggest.managers.GuiManager;
import org.bukkit.plugin.java.JavaPlugin;

public class ProSuggest extends JavaPlugin {

    private static ProSuggest instance;
    private ConfigManager configManager;
    private SuggestionManager suggestionManager;
    private GuiManager guiManager;
    private ChatListener chatListener;

    @Override
    public void onEnable() {
        instance = this;

        // Initialize managers
        this.configManager = new ConfigManager(this);
        this.suggestionManager = new SuggestionManager(this);
        this.guiManager = new GuiManager(this);

        // Load configurations
        configManager.loadConfigs();
        suggestionManager.loadSuggestions();

        // Register commands
        registerCommands();

        // Register listeners
        registerListeners();

        getLogger().info("ProSuggest has been enabled successfully!");
    }

    @Override
    public void onDisable() {
        // Save suggestions
        if (suggestionManager != null) {
            suggestionManager.saveSuggestions();
        }

        getLogger().info("ProSuggest has been disabled!");
    }

    private void registerCommands() {
        getCommand("suggest").setExecutor(new SuggestCommand(this));
        getCommand("psadmin").setExecutor(new PsAdminCommand(this));
    }

    private void registerListeners() {
        this.chatListener = new ChatListener(this);
        getServer().getPluginManager().registerEvents(new GuiListener(this), this);
        getServer().getPluginManager().registerEvents(chatListener, this);
    }

    public void reload() {
        configManager.loadConfigs();
        suggestionManager.loadSuggestions();
    }

    // Getters
    public static ProSuggest getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public SuggestionManager getSuggestionManager() {
        return suggestionManager;
    }

    public GuiManager getGuiManager() {
        return guiManager;
    }

    public ChatListener getChatListener() {
        return chatListener;
    }
}
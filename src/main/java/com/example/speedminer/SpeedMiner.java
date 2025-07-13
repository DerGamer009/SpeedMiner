package com.example.speedminer;

import com.example.speedminer.database.DatabaseManager;
import com.example.speedminer.game.GameManager;
import com.example.speedminer.game.ArenaManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;

public class SpeedMiner extends JavaPlugin {

    private GameManager gameManager;
    private DatabaseManager databaseManager;
    private ArenaManager arenaManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        databaseManager = new DatabaseManager(this);
        arenaManager = new ArenaManager(this);
        gameManager = new GameManager(this, arenaManager);

        Bukkit.getConsoleSender().sendMessage(ChatColor.GREEN + "[SpeedMiner] Plugin enabled!");

        if (getCommand("speedminer") != null) {
            getCommand("speedminer").setExecutor(gameManager);
        }
        getServer().getPluginManager().registerEvents(gameManager, this);
    }

    @Override
    public void onDisable() {
        if (gameManager != null) {
            gameManager.shutdown();
        }
        if (databaseManager != null) {
            databaseManager.close();
        }

        Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "[SpeedMiner] Plugin disabled!");
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public ArenaManager getArenaManager() {
        return arenaManager;
    }
}

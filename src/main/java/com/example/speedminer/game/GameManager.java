package com.example.speedminer.game;

import com.example.speedminer.SpeedMiner;
import com.example.speedminer.database.DatabaseManager;
import com.example.speedminer.game.ArenaManager;
import com.example.speedminer.game.ArenaManager.Arena;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class GameManager implements CommandExecutor, Listener {

    private final SpeedMiner plugin;
    private final PointsManager pointsManager;
    private final Set<UUID> participants = new HashSet<>();
    private final ScoreboardManager scoreboardManager;
    private final ArenaManager arenaManager;
    private BukkitRunnable gameTask;

    public GameManager(SpeedMiner plugin, ArenaManager arenaManager) {
        this.plugin = plugin;
        this.pointsManager = new PointsManager();
        this.scoreboardManager = new ScoreboardManager(plugin);
        this.arenaManager = arenaManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(ChatColor.RED + "Usage: /speedminer <start|stop|stats>");
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "start":
                startGame(sender);
                break;
            case "stop":
                stopGame(sender);
                break;
            case "stats":
                if (args.length >= 2) {
                    showStats(sender, args[1]);
                } else {
                    sender.sendMessage(ChatColor.RED + "Usage: /speedminer stats <player>");
                }
                break;
            case "top":
                showTop(sender);
                break;
            default:
                sender.sendMessage(ChatColor.RED + "Unknown subcommand");
                break;
        }
        return true;
    }

    private void startGame(CommandSender sender) {
        if (gameTask != null) {
            sender.sendMessage(ChatColor.RED + "Game already running!");
            return;
        }
        participants.clear();
        for (Player player : Bukkit.getOnlinePlayers()) {
            participants.add(player.getUniqueId());
        }
        pointsManager.clear();
        int countdown = plugin.getConfig().getInt("game.countdown", 5);
        Bukkit.broadcastMessage(ChatColor.GREEN + "SpeedMiner starting in " + countdown + " seconds!");
        new BukkitRunnable() {
            int time = countdown;
            @Override
            public void run() {
                if (time <= 0) {
                    cancel();
                    beginGame();
                    return;
                }
                Bukkit.broadcastMessage(ChatColor.YELLOW + "Starting in " + time + "...");
                time--;
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    private void beginGame() {
        teleportParticipants();
        int duration = plugin.getConfig().getInt("game.duration", 180);
        Bukkit.broadcastMessage(ChatColor.GREEN + "Go mine those ores!");
        scoreboardManager.start(participants, duration);
        gameTask = new BukkitRunnable() {
            int time = duration;
            @Override
            public void run() {
                if (time <= 0) {
                    endGame();
                    return;
                }
                time--;
            }
        };
        gameTask.runTaskTimer(plugin, 0L, 20L);
    }

    private void stopGame(CommandSender sender) {
        if (gameTask == null) {
            sender.sendMessage(ChatColor.RED + "Game is not running!");
            return;
        }
        endGame();
        sender.sendMessage(ChatColor.GREEN + "Game stopped.");
    }

    private void endGame() {
        if (gameTask != null) {
            gameTask.cancel();
            gameTask = null;
        }
        scoreboardManager.stop();
        Bukkit.broadcastMessage(ChatColor.GOLD + "SpeedMiner finished!");
        List<Map.Entry<UUID, Integer>> sorted = new ArrayList<>(pointsManager.getAll().entrySet());
        sorted.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));

        DatabaseManager db = plugin.getDatabaseManager();
        for (int i = 0; i < sorted.size(); i++) {
            UUID uuid = sorted.get(i).getKey();
            int points = sorted.get(i).getValue();
            String name = Bukkit.getOfflinePlayer(uuid).getName();
            Bukkit.broadcastMessage(ChatColor.AQUA + (i + 1) + ". " + name + " - " + points + " points");
            db.saveStats(uuid, name, points);
        }
    }

    private void showStats(CommandSender sender, String target) {
        DatabaseManager db = plugin.getDatabaseManager();
        int points = db.loadStats(target);
        sender.sendMessage(ChatColor.AQUA + target + " has " + points + " total points.");
    }

    private void showTop(CommandSender sender) {
        DatabaseManager db = plugin.getDatabaseManager();
        List<DatabaseManager.PlayerStat> top = db.getTopPlayers(5);
        sender.sendMessage(ChatColor.GOLD + "--- Top Players ---");
        int rank = 1;
        for (DatabaseManager.PlayerStat stat : top) {
            sender.sendMessage(ChatColor.YELLOW + "" + rank + ". " + stat.name() + " - " + stat.points() + " points");
            rank++;
        }
    }

    private void teleportParticipants() {
        Arena arena = arenaManager.getRandomArena();
        if (arena == null) return;
        Location loc = arena.toLocation();
        if (loc == null) return;
        for (UUID uuid : participants) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                player.teleport(loc);
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (gameTask == null) return;
        UUID uuid = event.getPlayer().getUniqueId();
        if (!participants.contains(uuid)) return;

        String blockName = event.getBlock().getType().name();
        int value = plugin.getConfig().getInt("points." + blockName, 0);
        if (value > 0) {
            event.setDropItems(false);
            pointsManager.addPoints(uuid, value);
            scoreboardManager.updatePoints(uuid, pointsManager.getPoints(uuid));
        }
    }

    public void shutdown() {
        if (gameTask != null) {
            gameTask.cancel();
        }
        scoreboardManager.stop();
    }
}

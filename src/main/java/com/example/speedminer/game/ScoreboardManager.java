package com.example.speedminer.game;

import com.example.speedminer.SpeedMiner;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ScoreboardManager {

    private final SpeedMiner plugin;
    private final Map<UUID, Scoreboard> boards = new HashMap<>();
    private BukkitRunnable task;
    private int timeLeft;

    public ScoreboardManager(SpeedMiner plugin) {
        this.plugin = plugin;
    }

    public void start(Collection<UUID> players, int duration) {
        this.timeLeft = duration;
        org.bukkit.scoreboard.ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager == null) return;
        for (UUID uuid : players) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null) continue;
            Scoreboard board = manager.getNewScoreboard();
            Objective obj = board.registerNewObjective("speedminer", "dummy", ChatColor.GREEN + "SpeedMiner");
            obj.setDisplaySlot(DisplaySlot.SIDEBAR);
            obj.getScore(ChatColor.AQUA + "Points").setScore(0);
            obj.getScore(ChatColor.YELLOW + "Time Left").setScore(duration);
            player.setScoreboard(board);
            boards.put(uuid, board);
        }
        task = new BukkitRunnable() {
            @Override
            public void run() {
                if (timeLeft < 0) {
                    cancel();
                    return;
                }
                for (Scoreboard board : boards.values()) {
                    Objective obj = board.getObjective("speedminer");
                    if (obj != null) {
                        obj.getScore(ChatColor.YELLOW + "Time Left").setScore(timeLeft);
                    }
                }
                timeLeft--;
            }
        };
        task.runTaskTimer(plugin, 0L, 20L);
    }

    public void updatePoints(UUID uuid, int points) {
        Scoreboard board = boards.get(uuid);
        if (board == null) return;
        Objective obj = board.getObjective("speedminer");
        if (obj != null) {
            obj.getScore(ChatColor.AQUA + "Points").setScore(points);
        }
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        for (UUID uuid : boards.keySet()) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null && Bukkit.getScoreboardManager() != null) {
                p.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
            }
        }
        boards.clear();
    }
}

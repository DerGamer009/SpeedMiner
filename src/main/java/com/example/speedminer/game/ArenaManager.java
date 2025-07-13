package com.example.speedminer.game;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class ArenaManager {

    public record Arena(String world, double x, double y, double z, float yaw, float pitch) {
        public Location toLocation() {
            if (world == null) return null;
            var w = Bukkit.getWorld(world);
            if (w == null) return null;
            return new Location(w, x, y, z, yaw, pitch);
        }
    }

    private final List<Arena> arenas = new ArrayList<>();
    private final Random random = new Random();

    public ArenaManager(JavaPlugin plugin) {
        var config = plugin.getConfig();
        ConfigurationSection list = config.getConfigurationSection("arenas");
        if (list != null) {
            for (String key : list.getKeys(false)) {
                ConfigurationSection sec = list.getConfigurationSection(key);
                if (sec != null) {
                    arenas.add(new Arena(
                            sec.getString("world", "world"),
                            sec.getDouble("x", 0.0),
                            sec.getDouble("y", 64.0),
                            sec.getDouble("z", 0.0),
                            (float) sec.getDouble("yaw", 0.0),
                            (float) sec.getDouble("pitch", 0.0)
                    ));
                }
            }
        }
        if (arenas.isEmpty()) {
            // fallback to single arena config
            arenas.add(new Arena(
                    config.getString("arena.world", "world"),
                    config.getDouble("arena.x", 0.0),
                    config.getDouble("arena.y", 64.0),
                    config.getDouble("arena.z", 0.0),
                    (float) config.getDouble("arena.yaw", 0.0),
                    (float) config.getDouble("arena.pitch", 0.0)
            ));
        }
    }

    public List<Arena> getArenas() {
        return Collections.unmodifiableList(arenas);
    }

    public Arena getRandomArena() {
        if (arenas.isEmpty()) return null;
        return arenas.get(random.nextInt(arenas.size()));
    }
}

package com.example.speedminer.game;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PointsManager {
    private final Map<UUID, Integer> points = new HashMap<>();

    public void addPoints(UUID uuid, int amount) {
        points.merge(uuid, amount, Integer::sum);
    }

    public int getPoints(UUID uuid) {
        return points.getOrDefault(uuid, 0);
    }

    public Map<UUID, Integer> getAll() {
        return Collections.unmodifiableMap(points);
    }

    public void clear() {
        points.clear();
    }
}

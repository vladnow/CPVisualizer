package ru.mamont.cpv;

import org.bukkit.Material;

public record SearchResult(String player, String world, int x, int y, int z, long timestamp,
                           ActionType action, Material block, String details) {
    public String positionKey() { return world + ":" + x + ":" + y + ":" + z; }
}

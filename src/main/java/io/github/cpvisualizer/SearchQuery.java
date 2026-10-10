package io.github.cpvisualizer;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import java.util.Map;

/** Captured on the server thread. World handles are passed only to CoreProtect lookup options. */
public record SearchQuery(String player, Material material, ActionType action, int seconds,
                          Scope scope, int radius, Location center, String worldName,
                          Map<String, World> worlds, ConfigManager settings) {
    public enum Scope { WORLD, ALL, RADIUS }
    /** null disables the corresponding restriction; it is never a synthetic Material/player. */
    public boolean allPlayers() { return player == null; }
    public boolean allBlocks() { return material == null; }
}

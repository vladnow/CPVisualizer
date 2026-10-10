package io.github.cpvisualizer;

import org.bukkit.Location;
import java.util.Comparator;
import java.util.List;

/** API v12 rejects a global legacy lookup without a positive radius. */
final class BlockLookupPlan {
    // All legal vanilla X/Z positions, independent of the movable world border.
    static final int WORLD_RADIUS = 30_000_000;
    record Area(Location center, int radius) { }
    private BlockLookupPlan() { }
    static List<Area> areas(SearchQuery q) {
        if (q.scope() == SearchQuery.Scope.RADIUS) return List.of(new Area(q.center().clone(), q.radius()));
        if (!q.allPlayers()) return List.of(new Area(q.scope() == SearchQuery.Scope.ALL ? null : q.center().clone(), -1));
        if (q.scope() == SearchQuery.Scope.WORLD) return List.of(new Area(origin(q.center()), WORLD_RADIUS));
        return q.worlds().entrySet().stream().sorted(Comparator.comparing(e -> e.getKey()))
            .map(e -> new Area(new Location(e.getValue(), 0, 0, 0), WORLD_RADIUS)).toList();
    }
    private static Location origin(Location center) {
        Location origin = center.clone(); origin.setX(0); origin.setY(0); origin.setZ(0); return origin;
    }
}

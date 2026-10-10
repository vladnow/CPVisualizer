package io.github.cpvisualizer;

import net.coreprotect.CoreProtect;
import net.coreprotect.CoreProtectAPI;
import net.coreprotect.api.LookupOptions;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import java.util.*;

public final class CoreProtectHook {
    private final CoreProtectAPI api;
    public CoreProtectHook() {
        var plugin = Bukkit.getPluginManager().getPlugin("CoreProtect");
        if (!(plugin instanceof CoreProtect cp) || !plugin.isEnabled())
            throw new IllegalStateException("CoreProtect is missing or disabled.");
        api = cp.getAPI();
        if (api == null || api.APIVersion() < 12 || !api.isEnabled())
            throw new IllegalStateException("CoreProtect API 12 or newer must be enabled (CoreProtect 24.1).");
    }
    public record Outcome(List<SearchResult> results, boolean limited, int unresolved) { }

    // Test boundary: use the same public API calls without starting a Minecraft server.
    CoreProtectHook(CoreProtectAPI api) { this.api = Objects.requireNonNull(api); }

    public Outcome lookup(SearchQuery q, Runnable checkpoint) {
        if (!api.isEnabled()) throw new IllegalStateException("CoreProtect API is disabled.");
        var cfg = q.settings();
        List<SearchResult> results = new ArrayList<>();
        boolean limited = false;
        int unresolved = 0;
        checkpoint.run();
        if (q.action().blocks()) {
            List<Integer> actions = switch(q.action()) {
                case PLACE -> List.of(1); case BREAK -> List.of(0); case INTERACT -> List.of(2);
                default -> List.of(0,1,2);
            };
            int scanLimit = q.allPlayers() ? cfg.blockScanLimit() : cfg.maxResults();
            for (var area : BlockLookupPlan.areas(q)) {
                checkpoint.run();
                var rows = api.performPartialLookup(q.seconds(), new ArrayList<>(List.of(q.allPlayers() ? "#global" : q.player())), null,
                    q.allBlocks() ? null : new ArrayList<>(List.of(q.material())), null, new ArrayList<>(actions),
                    area.radius(), area.center(), 0, scanLimit+1);
                if (rows == null) throw new IllegalStateException("CoreProtect rejected the block lookup.");
                limited |= rows.size() > scanLimit;
                for (var row : rows.subList(0,Math.min(rows.size(),scanLimit))) {
                    checkpoint.run();
                    var r = api.parseResult(row);
                    if (q.allPlayers() && !FilterSelection.isPlayerActor(r.getPlayer())) continue;
                    ActionType action = switch(r.getActionId()) { case 0 -> ActionType.BREAK; case 1 -> ActionType.PLACE; default -> ActionType.INTERACT; };
                    results.add(new SearchResult(r.getPlayer(), r.worldName(), r.getX(), r.getY(), r.getZ(),
                        r.getTimestamp(), action, r.getType(), "Rolled back: " + r.isRolledBack()));
                }
                // Bound intermediate storage while merging all loaded worlds.
                results.sort(Comparator.comparingLong(SearchResult::timestamp).reversed());
                if (results.size() > cfg.maxResults()) {
                    limited = true; results.subList(cfg.maxResults(),results.size()).clear();
                }
            }
        }
        if (q.action().containers()) {
            checkpoint.run();
            // v12 filters user/time/radius in SQL. Material here would mean the ITEM, not container.
            var options = LookupOptions.builder().user(q.player()).time(q.seconds()).limit(0,cfg.scanLimit()+1);
            if (q.scope() == SearchQuery.Scope.RADIUS) options.radius(q.center(),q.radius());
            var rows = api.containerLookup(options.build());
            if (rows == null) throw new IllegalStateException("CoreProtect rejected the container lookup.");
            limited |= rows.size() > cfg.scanLimit();
            Map<String,List<HistoryResolver.Evidence>> cache = new HashMap<>();
            int scanned = 0;
            for (var r : rows) {
                checkpoint.run();
                if (++scanned > cfg.scanLimit()) break;
                if (q.allPlayers() && !FilterSelection.isPlayerActor(r.getPlayer())) continue;
                if (q.scope() != SearchQuery.Scope.ALL && !q.worldName().equals(r.worldName())) continue;
                if (r.getActionId() != 0 && r.getActionId() != 1) continue;
                ActionType action = r.getActionId() == 1 ? ActionType.CONTAINER_ADD : ActionType.CONTAINER_REMOVE;
                if (q.action() != ActionType.ALL && q.action() != action) continue;
                String key = r.worldName()+":"+r.getX()+":"+r.getY()+":"+r.getZ();
                var evidence = cache.get(key);
                if (evidence == null) {
                    var world = q.worlds().get(r.worldName());
                    if (world == null || cache.size() >= cfg.historyPositions()) {
                        unresolved++;
                        if (!q.allBlocks()) { limited = true; continue; }
                        results.add(containerResult(r,action,null));
                        continue;
                    }
                    // Public v12 bounded block lookup, all actors, small square; post-filter exact XYZ.
                    var history = api.performPartialLookup(Integer.MAX_VALUE,new ArrayList<>(List.of("#global")),null,
                        null,null,new ArrayList<>(List.of(0,1,2)),1,new Location(world,r.getX(),r.getY(),r.getZ()),0,cfg.historyRows());
                    if (history == null) throw new IllegalStateException("CoreProtect rejected the position history lookup.");
                    limited |= history.size() == cfg.historyRows();
                    evidence = new ArrayList<>();
                    for (var raw : history) {
                        checkpoint.run();
                        var h = api.parseResult(raw);
                        if (h.getX()!=r.getX() || h.getY()!=r.getY() || h.getZ()!=r.getZ() || !h.worldName().equals(r.worldName())) continue;
                        evidence.add(new HistoryResolver.Evidence(h.getTimestamp(),h.getActionId(),h.getType()==null?null:h.getType().name(),h.isRolledBack()));
                    }
                    cache.put(key,evidence);
                }
                String historical = HistoryResolver.materialAt(evidence,r.getTimestamp());
                if (historical == null) { unresolved++; if (!q.allBlocks()) continue; }
                if (!q.allBlocks() && !q.material().name().equals(historical)) continue;
                Material block = historical == null ? null : Material.matchMaterial(historical);
                results.add(containerResult(r,action,block));
            }
        }
        checkpoint.run();
        results.sort(Comparator.comparingLong(SearchResult::timestamp).reversed());
        limited |= results.size() > cfg.maxResults();
        return new Outcome(List.copyOf(results.subList(0,Math.min(results.size(),cfg.maxResults()))),limited,unresolved);
    }

    private SearchResult containerResult(net.coreprotect.api.result.ContainerResult r,ActionType action,Material block) {
        var meta = r.getMetadata();
        return new SearchResult(r.getPlayer(),r.worldName(),r.getX(),r.getY(),r.getZ(),r.getTimestamp(),action,block,
            "Container type: " + (block == null ? "unknown" : "from history") + "; item: "+r.getType()+" ×"+r.getAmount()+
            "; rolled back: "+r.isRolledBack()+"; metadata bytes: "+(meta==null?0:meta.length));
    }
}

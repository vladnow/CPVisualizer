package ru.mamont.cpv;

import org.bukkit.Material;
import java.util.List;

/** Main-thread confined. Workers receive immutable query snapshots instead. */
public final class PlayerSearchSession {
    String target;
    Material block = Material.CHEST;
    ActionType action = ActionType.ALL;
    int seconds;
    int radius = 100;
    SearchQuery.Scope scope = SearchQuery.Scope.WORLD;
    List<SearchResult> results = List.of();
    int selected;
    long lastSearch;
    long visibleUntil;
    long revision;
    SearchManager.Job job;
    boolean teleporting;
    PlayerSearchSession(String target, int seconds) { this.target = target; this.seconds = seconds; }
    public SearchResult current() { return results.isEmpty() ? null : results.get(selected); }
}

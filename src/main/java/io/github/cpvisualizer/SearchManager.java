package io.github.cpvisualizer;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

public final class SearchManager implements AutoCloseable {
    private final CoreProtectVisualizerPlugin plugin;
    private final CoreProtectHook hook;
    private final ThreadPoolExecutor pool;
    private volatile boolean closed;
    public static final class Job {
        final AtomicBoolean cancelled = new AtomicBoolean();
        final long deadline;
        Runnable task;
        Job(int timeout) { deadline = System.nanoTime()+TimeUnit.SECONDS.toNanos(timeout); }
        void check() {
            if (cancelled.get() || Thread.currentThread().isInterrupted()) throw new CancellationException();
            if (System.nanoTime() > deadline) throw new IllegalStateException("Search timed out; narrow the period or scope.");
        }
    }
    public SearchManager(CoreProtectVisualizerPlugin plugin,CoreProtectHook hook,ConfigManager cfg) {
        this.plugin=plugin; this.hook=hook;
        pool=new ThreadPoolExecutor(cfg.workers(),cfg.workers(),0,TimeUnit.SECONDS,new ArrayBlockingQueue<>(cfg.queue()),r->{
            Thread t=new Thread(r,"CPVisualizer-lookup"); t.setDaemon(true); return t;
        },new ThreadPoolExecutor.AbortPolicy());
    }
    public void cancel(PlayerSearchSession s) {
        if(s.job!=null) { s.job.cancelled.set(true); pool.remove(s.job.task); s.job=null; }
        s.revision++;
    }
    public void search(Player p) {
        if(!plugin.allowed(p,"search")) return;
        var s=plugin.session(p); var cfg=plugin.settings();
        long now=System.currentTimeMillis();
        if(now-s.lastSearch<cfg.cooldown()) { plugin.tell(p,"Please wait before starting another search."); return; }
        if(s.seconds>cfg.maxTime()) { plugin.tell(p,"The period exceeds search.max-time."); return; }
        cancel(s);
        s.lastSearch=now;
        Map<String,World> worlds=new HashMap<>();
        Bukkit.getWorlds().forEach(w->worlds.put(w.getName(),w));
        var query=new SearchQuery(s.target,s.block,s.action,s.seconds,s.scope,s.radius,p.getLocation().clone(),
            p.getWorld().getName(),Map.copyOf(worlds),cfg);
        UUID owner=p.getUniqueId();
        Job job=new Job(cfg.timeout()); s.job=job;
        job.task=()->{
            long started=System.nanoTime();
            try {
                var outcome=hook.lookup(query,job::check);
                complete(()->{
                    Player online=Bukkit.getPlayer(owner);
                    if(online==null || s.job!=job || job.cancelled.get()) return;
                    s.job=null;
                    if(!plugin.allowed(online,"search")) return;
                    plugin.renderer().clear(owner);
                    s.results=outcome.results(); s.selected=0; s.revision++;
                    s.visibleUntil=System.currentTimeMillis()+cfg.lifetime()*1000L;
                    if(!query.allPlayers())plugin.remember(query.player());
                    outcome.results().stream().map(SearchResult::player).filter(FilterSelection::isPlayerActor).forEach(plugin::remember);
                    plugin.tell(online,"Found: "+s.results.size()+"."+(outcome.limited()?" Results are limited; narrow the filters.":"")+
                        (outcome.unresolved()>0?" Unresolved container types: "+outcome.unresolved()+".":""));
                    if(query.allPlayers() && query.scope()==SearchQuery.Scope.ALL && query.action().blocks())
                        plugin.tell(online,"All players: block history was checked in "+query.worlds().size()+" loaded worlds. API 12 does not support unloaded worlds for this lookup.");
                    plugin.getLogger().info(online.getName()+" search returned "+s.results.size()+" results in "+
                        TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-started)+" ms.");
                    plugin.gui().results(online,0);
                });
            } catch(CancellationException ignored) {
                // The obsolete query never publishes its results. Its occupied worker remains occupied until API returns.
            } catch(Exception ex) {
                if(!job.cancelled.get()) {
                    plugin.getLogger().log(Level.WARNING,"CoreProtect lookup failed for "+(query.allPlayers()?"all players":query.player()),ex);
                    complete(()->{ Player online=Bukkit.getPlayer(owner); if(s.job==job) { s.job=null;
                        if(online!=null) plugin.tell(online,"Search failed. "+ex.getMessage()); } });
                }
            }
        };
        try {
            pool.execute(job.task); plugin.tell(p,"Search started...");
            Bukkit.getScheduler().runTaskLater(plugin,()->{
                if(s.job==job) {
                    cancel(s);
                    Player online=Bukkit.getPlayer(owner);
                    if(online!=null) plugin.tell(online,"Search timed out. Narrow the period or scope. Late results will be discarded.");
                }
            },cfg.timeout()*20L);
        }
        catch(RejectedExecutionException ex) { s.job=null; plugin.tell(p,"The search queue is full. Try again later."); }
    }
    private void complete(Runnable task) {
        if(closed) return;
        try { Bukkit.getScheduler().runTask(plugin,()->{if(!closed) task.run();}); }
        catch(org.bukkit.plugin.IllegalPluginAccessException ignored) { }
    }
    @Override public void close() { closed=true; pool.shutdownNow(); }
}

package ru.mamont.cpv;

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
            if (System.nanoTime() > deadline) throw new IllegalStateException("Истекло время поиска; уменьшите период или область.");
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
        if(now-s.lastSearch<cfg.cooldown()) { plugin.tell(p,"Подождите перед новым запросом."); return; }
        if(s.seconds>cfg.maxTime()) { plugin.tell(p,"Период превышает search.max-time."); return; }
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
                    plugin.tell(online,"Найдено: "+s.results.size()+"."+(outcome.limited()?" Выборка ограничена; уточните фильтры.":"")+
                        (outcome.unresolved()>0?" Без подтверждённого типа контейнера: "+outcome.unresolved()+".":""));
                    if(query.allPlayers() && query.scope()==SearchQuery.Scope.ALL && query.action().blocks())
                        plugin.tell(online,"Все игроки: блоковая история проверена в "+query.worlds().size()+" загруженных мирах. Незагруженные миры API v12 не позволяет опросить этим способом.");
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
                        if(online!=null) plugin.tell(online,"Поиск не выполнен. "+ex.getMessage()); } });
                }
            }
        };
        try {
            pool.execute(job.task); plugin.tell(p,"Поиск запущен…");
            Bukkit.getScheduler().runTaskLater(plugin,()->{
                if(s.job==job) {
                    cancel(s);
                    Player online=Bukkit.getPlayer(owner);
                    if(online!=null) plugin.tell(online,"Истекло время поиска. Уменьшите период или область. Ответ завершившегося позже запроса будет отброшен.");
                }
            },cfg.timeout()*20L);
        }
        catch(RejectedExecutionException ex) { s.job=null; plugin.tell(p,"Очередь поиска заполнена. Повторите позже."); }
    }
    private void complete(Runnable task) {
        if(closed) return;
        try { Bukkit.getScheduler().runTask(plugin,()->{if(!closed) task.run();}); }
        catch(org.bukkit.plugin.IllegalPluginAccessException ignored) { }
    }
    @Override public void close() { closed=true; pool.shutdownNow(); }
}

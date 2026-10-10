package io.github.cpvisualizer;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import java.util.Set;

public final class NavigationManager {
    private final CoreProtectVisualizerPlugin plugin;
    private static final Set<Material> DANGER=Set.of(Material.LAVA,Material.FIRE,Material.SOUL_FIRE,Material.CACTUS,
        Material.MAGMA_BLOCK,Material.CAMPFIRE,Material.SOUL_CAMPFIRE,Material.POWDER_SNOW,Material.SWEET_BERRY_BUSH,Material.WITHER_ROSE);
    public NavigationManager(CoreProtectVisualizerPlugin plugin) { this.plugin=plugin; }
    public void select(Player p,int index) {
        var s=plugin.session(p);
        if(s.results.isEmpty()) {plugin.tell(p,"No results. /cpv search");return;}
        s.selected=Math.floorMod(index,s.results.size()); s.revision++;
        s.visibleUntil=System.currentTimeMillis()+plugin.settings().lifetime()*1000L;
        plugin.tell(p,"Result "+(s.selected+1)+" / "+s.results.size()+" — "+plugin.brief(s.current()));
    }
    public void teleport(Player p) {
        if(!plugin.allowed(p,"teleport")) return;
        var s=plugin.session(p); var r=s.current();
        if(r==null) {plugin.tell(p,"No result selected.");return;}
        if(s.teleporting) {plugin.tell(p,"Teleportation is already in progress.");return;}
        var world=Bukkit.getWorld(r.world());
        if(world==null || r.y()<world.getMinHeight() || r.y()>=world.getMaxHeight()) {plugin.tell(p,"The world is unavailable or the coordinates exceed its height limits.");return;}
        var target=new Location(world,r.x()+.5,r.y()+1,r.z()+.5,p.getYaw(),p.getPitch());
        if(!world.getWorldBorder().isInside(target)) {plugin.tell(p,"The result is outside the world border.");return;}
        long revision=s.revision; s.teleporting=true;
        // Do not generate previously ungenerated chunks during investigation.
        world.getChunkAtAsync(r.x()>>4,r.z()>>4,false).whenComplete((chunk,error)->sync(()->{
            if(!p.isOnline() || plugin.sessions().get(p.getUniqueId())!=s || s.revision!=revision || !p.hasPermission("cpvisualizer.use") || !p.hasPermission("cpvisualizer.search") || !p.hasPermission("cpvisualizer.teleport")) {s.teleporting=false;return;}
            if(error!=null || chunk==null) {s.teleporting=false;plugin.tell(p,"Could not load an existing destination chunk.");return;}
            Location safe=p.getGameMode()==GameMode.SPECTATOR?target:findSafe(target);
            if(safe==null) {s.teleporting=false;plugin.tell(p,"No safe location nearby. Use spectator mode and retry /cpv tp.");return;}
            p.teleportAsync(safe,PlayerTeleportEvent.TeleportCause.PLUGIN).whenComplete((ok,ex)->sync(()->{
                s.teleporting=false;
                if(p.isOnline()) plugin.tell(p,ex==null && Boolean.TRUE.equals(ok)?"Teleported successfully.":"Teleportation was cancelled or failed.");
            }));
        }));
    }
    private Location findSafe(Location target) {
        var w=target.getWorld();
        for(int dy=0;dy<=6;dy++) for(int dx=-3;dx<=3;dx++) for(int dz=-3;dz<=3;dz++) {
            int x=target.getBlockX()+dx,y=target.getBlockY()+dy,z=target.getBlockZ()+dz;
            if(y<=w.getMinHeight() || y+1>=w.getMaxHeight() || !w.isChunkLoaded(x>>4,z>>4)) continue;
            Block floor=w.getBlockAt(x,y-1,z), feet=w.getBlockAt(x,y,z), head=w.getBlockAt(x,y+1,z);
            var result=new Location(w,x+.5,y,z+.5,target.getYaw(),target.getPitch());
            if(floor.getType().isSolid() && !DANGER.contains(floor.getType()) && clear(feet) && clear(head) && w.getWorldBorder().isInside(result)) return result;
        }
        return null;
    }
    private boolean clear(Block b) {return b.isEmpty() && !b.isLiquid() && !DANGER.contains(b.getType());}
    private void sync(Runnable task) {
        if(!plugin.isEnabled()) return;
        try {Bukkit.getScheduler().runTask(plugin,task);} catch(org.bukkit.plugin.IllegalPluginAccessException ignored) { }
    }
}

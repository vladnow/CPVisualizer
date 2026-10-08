package ru.mamont.cpv;

import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import java.util.*;

public final class VisualizationManager implements ResultRenderer {
    private final CoreProtectVisualizerPlugin plugin;
    private final Map<UUID,Map<String,BlockDisplay>> displays=new HashMap<>();
    public VisualizationManager(CoreProtectVisualizerPlugin plugin) { this.plugin=plugin; }
    @Override public void tick() {
        var cfg=plugin.settings();
        for(var entry:plugin.sessions().entrySet()) {
            UUID id=entry.getKey(); var s=entry.getValue(); var p=Bukkit.getPlayer(id);
            if(p==null || !p.hasPermission("cpvisualizer.use") || !p.hasPermission("cpvisualizer.search") || System.currentTimeMillis()>s.visibleUntil) {
                clear(id); continue;
            }
            var wanted=new LinkedHashMap<String,SearchResult>();
            List<SearchResult> ordered=new ArrayList<>();
            if(s.current()!=null) ordered.add(s.current());
            ordered.addAll(s.results);
            var here=p.getLocation(); var world=p.getWorld();
            for(var r:ordered) {
                if(wanted.size()>=cfg.visible()) break;
                if(!world.getName().equals(r.world()) || r.y()<world.getMinHeight() || r.y()>=world.getMaxHeight()) continue;
                var at=new Location(world,r.x()+.5,r.y()+.5,r.z()+.5);
                if(here.distanceSquared(at)>cfg.distance()*(double)cfg.distance() || !world.isChunkLoaded(r.x()>>4,r.z()>>4)) continue;
                wanted.putIfAbsent(r.positionKey(),r);
            }
            var own=displays.computeIfAbsent(id,k->new HashMap<>());
            own.entrySet().removeIf(e->{if(!wanted.containsKey(e.getKey()) || !e.getValue().isValid()) {e.getValue().remove(); return true;} return false;});
            int spawned=0;
            for(var r:wanted.values()) {
                var d=own.get(r.positionKey());
                if(d==null) {
                    if(spawned++>=10) continue;
                    d=world.spawn(new Location(world,r.x(),r.y(),r.z()),BlockDisplay.class,entity->{
                        // Applied BEFORE adding the entity to the world: no one-frame visibility leak.
                        entity.setVisibleByDefault(false); entity.setPersistent(false);
                        entity.setGravity(false); entity.setInvulnerable(true);
                        entity.setBlock(Material.WHITE_STAINED_GLASS.createBlockData());
                        entity.setGlowing(true); entity.setBrightness(new Display.Brightness(15,15));
                        entity.setViewRange(cfg.distance()/64f); entity.setShadowRadius(0);
                    });
                    own.put(r.positionKey(),d); p.showEntity(plugin,d);
                }
                boolean selected=s.current()!=null && s.current().positionKey().equals(r.positionKey());
                float scale=selected?1.06f:1.015f;
                d.setGlowColorOverride(cfg.ageColor(r.timestamp()));
                d.setTransformation(new Transformation(new Vector3f((1-scale)/2),new Quaternionf(),new Vector3f(scale),new Quaternionf()));
            }
            SearchResult focus=null; double best=Double.MAX_VALUE;
            var eye=p.getEyeLocation(); var direction=eye.getDirection();
            for(var r:wanted.values()) {
                var vector=new org.bukkit.util.Vector(r.x()+.5,r.y()+.5,r.z()+.5).subtract(eye.toVector());
                double distance=vector.lengthSquared();
                if(distance<best && (distance<9 || (distance<256 && vector.clone().normalize().dot(direction)>.985))) {focus=r;best=distance;}
            }
            if(focus!=null) p.sendActionBar(Component.text(plugin.brief(focus)));
        }
    }
    @Override public void clear(UUID owner) {
        var own=displays.remove(owner); if(own!=null) own.values().forEach(BlockDisplay::remove);
    }
    @Override public void close() { new ArrayList<>(displays.keySet()).forEach(this::clear); }
}

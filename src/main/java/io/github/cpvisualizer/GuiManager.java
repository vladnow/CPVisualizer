package io.github.cpvisualizer;

import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.*;
import java.util.*;

public final class GuiManager implements Listener {
    private final CoreProtectVisualizerPlugin plugin;
    private static final String[] TIMES={"15m","1h","6h","12h","24h","3d","7d","30d"};
    private static final class Menu implements InventoryHolder {
        final UUID owner; final String type; final int page; final long revision;
        final Map<Integer,Runnable> clicks=new HashMap<>();
        Inventory inventory;
        Menu(Player p,String type,int page,long revision) {this.owner=p.getUniqueId();this.type=type;this.page=page;this.revision=revision;}
        @Override public Inventory getInventory(){return inventory;}
    }
    public GuiManager(CoreProtectVisualizerPlugin plugin){this.plugin=plugin;}
    private Menu menu(Player p,String type,int page,String title) {
        Menu m=new Menu(p,type,page,plugin.session(p).revision);
        m.inventory=Bukkit.createInventory(m,54,Component.text(title));return m;
    }
    private void button(Menu m,int slot,Material type,String title,List<String> lore,Runnable click) {
        if(!type.isItem() || type.isAir()) type=Material.PAPER;
        var item=new ItemStack(type);var meta=item.getItemMeta();
        meta.displayName(Component.text(title));meta.lore(lore.stream().map(Component::text).toList());item.setItemMeta(meta);
        m.inventory.setItem(slot,item);m.clicks.put(slot,click);
    }
    private void hint(Player p,String text){p.closeInventory();plugin.tell(p,text);}
    public void main(Player p) {
        if(!plugin.allowed(p,"use"))return;
        var s=plugin.session(p);var m=menu(p,"main",0,"CPVisualizer - filters");
        button(m,10,Material.PLAYER_HEAD,"Player: "+(s.target==null?"All players":s.target),List.of("Select a player, all players, or enter a name"),()->players(p,0));
        button(m,12,s.block==null?Material.GRASS_BLOCK:s.block,"Block: "+(s.block==null?"All blocks":s.block),List.of("Select a material or all blocks","Filter: /cpv blocks chest"),()->blocks(p,0,""));
        button(m,14,Material.IRON_PICKAXE,"Action: "+s.action,List.of("Click to cycle"),()->{
            s.action=ActionType.values()[(s.action.ordinal()+1)%ActionType.values().length];main(p);});
        button(m,16,Material.CLOCK,"Time: "+s.seconds+" seconds",List.of("Presets and custom input"),()->times(p));
        button(m,28,Material.COMPASS,"Scope: "+s.scope,List.of("WORLD → ALL → RADIUS","Radius: "+s.radius+" (X/Z square)"),()->{
            s.scope=SearchQuery.Scope.values()[(s.scope.ordinal()+1)%SearchQuery.Scope.values().length];main(p);});
        button(m,30,Material.SPYGLASS,"Radius: "+s.radius,List.of("Click: 50 / 100 / 500 / 1000 / 5000","Custom radius: /cpv radius <number>"),()->{
            int[] radii={50,100,500,1000,5000};int next=50;for(int n:radii) if(n>s.radius){next=n;break;}
            s.radius=Math.min(next,plugin.settings().maxRadius());s.scope=SearchQuery.Scope.RADIUS;main(p);});
        button(m,32,Material.LIME_DYE,"Start search",List.of("Search recorded history"),()->{p.closeInventory();plugin.search().search(p);});
        button(m,34,Material.BOOK,"Results: "+s.results.size(),List.of("Open saved results"),()->results(p,0));
        button(m,49,Material.BARRIER,"Clear",List.of("Cancel the search and remove highlights"),()->{plugin.clear(p);main(p);});
        p.openInventory(m.inventory);
    }
    private void times(Player p) {
        var m=menu(p,"time",0,"Search period");var s=plugin.session(p);
        for(int i=0;i<TIMES.length;i++) {
            String time=TIMES[i];
            button(m,i,Material.CLOCK,time,List.of(),()->{
                int seconds=TimeParser.seconds(time);
                if(seconds>plugin.settings().maxTime()) {plugin.tell(p,"The maximum search period was exceeded.");return;}
                s.seconds=seconds;main(p);
            });
        }
        button(m,45,Material.OAK_SIGN,"Custom input",List.of("/cpv time 6h"),()->hint(p,"Enter /cpv time 6h (also supported: 1d6h)."));
        button(m,49,Material.ARROW,"Back",List.of(),()->main(p));p.openInventory(m.inventory);
    }
    public void blocks(Player p,int page,String filter) {
        var all=Arrays.stream(Material.values()).filter(Material::isBlock).filter(x->!x.isAir())
            .filter(x->x.name().toLowerCase(Locale.ROOT).contains(filter.toLowerCase(Locale.ROOT))).sorted(Comparator.comparing(Enum::name)).toList();
        int current=Math.max(0,Math.min(page,Math.max(0,(all.size()-1)/45)));
        var m=menu(p,"blocks",current,"Blocks - "+(current+1));
        for(int i=current*45;i<Math.min(all.size(),current*45+45);i++) {
            Material mat=all.get(i);button(m,i%45,mat,mat.name(),List.of("Select a historical block material"),()->{plugin.session(p).block=mat;main(p);});
        }
        pages(m,p,current,all.size(),n->blocks(p,n,filter));
        button(m,50,Material.NETHER_STAR,"All blocks",List.of("Search without a block material filter","/cpv block all"),()->{plugin.session(p).block=null;main(p);});
        button(m,48,Material.OAK_SIGN,"Filter by name",List.of("/cpv blocks chest","Exact selection: /cpv block redstone_wire"),()->hint(p,"Enter /cpv blocks <filter> or /cpv block <material>."));
        p.openInventory(m.inventory);
    }
    private void players(Player p,int page) {
        var names=new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
        Bukkit.getOnlinePlayers().forEach(x->names.add(x.getName()));names.addAll(plugin.knownNames());
        var all=new ArrayList<>(names);int current=Math.max(0,Math.min(page,Math.max(0,(all.size()-1)/45)));
        var m=menu(p,"players",current,"Players - "+(current+1));
        for(int i=current*45;i<Math.min(all.size(),current*45+45);i++) {
            String name=all.get(i);button(m,i%45,Material.PLAYER_HEAD,name,List.of("Select"),()->{plugin.session(p).target=name;main(p);});
        }
        pages(m,p,current,all.size(),n->players(p,n));
        button(m,50,Material.NETHER_STAR,"All players",List.of("Recorded history of online and offline players","System actors such as #fire and #tnt are excluded","/cpv player all"),()->{plugin.session(p).target=null;main(p);});
        button(m,48,Material.OAK_SIGN,"Enter a historical player name",List.of("/cpv player ExamplePlayer","Offline players are supported"),()->hint(p,"Enter /cpv player <name>. The player does not need to be online."));
        p.openInventory(m.inventory);
    }
    public void results(Player p,int page) {
        if(!plugin.allowed(p,"search"))return;
        var s=plugin.session(p);int current=Math.max(0,Math.min(page,Math.max(0,(s.results.size()-1)/45)));
        var m=menu(p,"results",current,"Results - "+(current+1));
        for(int i=current*45;i<Math.min(s.results.size(),current*45+45);i++) {
            int index=i;var r=s.results.get(i);
            button(m,i%45,r.block()==null?Material.PAPER:r.block(),"#"+(i+1)+" "+r.player()+" — "+(r.block()==null?"Container: unknown type":r.block()),
                List.of(plugin.time(r.timestamp()),r.action().name(),r.world()+" "+r.x()+" "+r.y()+" "+r.z(),"Click to select and teleport"),()->{
                    plugin.navigation().select(p,index);p.closeInventory();plugin.navigation().teleport(p);
                });
        }
        pages(m,p,current,s.results.size(),n->results(p,n));p.openInventory(m.inventory);
    }
    private void pages(Menu m,Player p,int current,int count,java.util.function.IntConsumer open) {
        if(current>0)button(m,45,Material.ARROW,"Previous page",List.of(),()->open.accept(current-1));
        if((current+1)*45<count)button(m,53,Material.ARROW,"Next page",List.of(),()->open.accept(current+1));
        button(m,49,Material.COMPASS,"Filters",List.of("Total: "+count),()->main(p));
    }
    @EventHandler public void click(InventoryClickEvent e) {
        if(!(e.getView().getTopInventory().getHolder() instanceof Menu m))return;
        e.setCancelled(true);
        if(!(e.getWhoClicked() instanceof Player p) || !p.getUniqueId().equals(m.owner))return;
        if(!plugin.allowed(p,"use") || (m.type.equals("results") && !plugin.allowed(p,"search"))) {p.closeInventory();return;}
        if(e.getRawSlot()<0 || e.getRawSlot()>=54 || !e.isLeftClick())return;
        var action=m.clicks.get(e.getRawSlot());if(action==null)return;
        Bukkit.getScheduler().runTask(plugin,()->{
            if(!p.isOnline() || p.getOpenInventory().getTopInventory()!=m.inventory || !p.hasPermission("cpvisualizer.use"))return;
            if(m.type.equals("results") && (plugin.session(p).revision!=m.revision || !p.hasPermission("cpvisualizer.search"))) {p.closeInventory();return;}
            action.run();
        });
    }
    @EventHandler public void drag(InventoryDragEvent e) {if(e.getView().getTopInventory().getHolder() instanceof Menu)e.setCancelled(true);}
    public void closeMenus() {for(var p:Bukkit.getOnlinePlayers())if(p.getOpenInventory().getTopInventory().getHolder() instanceof Menu)p.closeInventory();}
}

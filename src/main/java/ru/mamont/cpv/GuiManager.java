package ru.mamont.cpv;

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
        var s=plugin.session(p);var m=menu(p,"main",0,"CPVisualizer — фильтры");
        button(m,10,Material.PLAYER_HEAD,"Игрок: "+(s.target==null?"Все игроки":s.target),List.of("Выбрать игрока / всех / ручной ввод"),()->players(p,0));
        button(m,12,s.block==null?Material.GRASS_BLOCK:s.block,"Блок: "+(s.block==null?"Все блоки":s.block),List.of("Выбрать блок или все блоки","Поиск: /cpv blocks chest"),()->blocks(p,0,""));
        button(m,14,Material.IRON_PICKAXE,"Действие: "+s.action,List.of("Нажмите, чтобы переключить"),()->{
            s.action=ActionType.values()[(s.action.ordinal()+1)%ActionType.values().length];main(p);});
        button(m,16,Material.CLOCK,"Время: "+s.seconds+" секунд",List.of("Предустановки и ручной ввод"),()->times(p));
        button(m,28,Material.COMPASS,"Область: "+s.scope,List.of("WORLD → ALL → RADIUS","Радиус: "+s.radius+" (квадрат X/Z)"),()->{
            s.scope=SearchQuery.Scope.values()[(s.scope.ordinal()+1)%SearchQuery.Scope.values().length];main(p);});
        button(m,30,Material.SPYGLASS,"Радиус: "+s.radius,List.of("Нажмите: 50 / 100 / 500 / 1000 / 5000","Ручной ввод: /cpv radius <число>"),()->{
            int[] radii={50,100,500,1000,5000};int next=50;for(int n:radii) if(n>s.radius){next=n;break;}
            s.radius=Math.min(next,plugin.settings().maxRadius());s.scope=SearchQuery.Scope.RADIUS;main(p);});
        button(m,32,Material.LIME_DYE,"Начать поиск",List.of("Асинхронный запрос CoreProtect"),()->{p.closeInventory();plugin.search().search(p);});
        button(m,34,Material.BOOK,"Результаты: "+s.results.size(),List.of("Открыть сохранённую выборку"),()->results(p,0));
        button(m,49,Material.BARRIER,"Очистить",List.of("Отменить поиск и удалить метки"),()->{plugin.clear(p);main(p);});
        p.openInventory(m.inventory);
    }
    private void times(Player p) {
        var m=menu(p,"time",0,"Период поиска");var s=plugin.session(p);
        for(int i=0;i<TIMES.length;i++) {
            String time=TIMES[i];
            button(m,i,Material.CLOCK,time,List.of(),()->{
                int seconds=TimeParser.seconds(time);
                if(seconds>plugin.settings().maxTime()) {plugin.tell(p,"Превышен максимальный период.");return;}
                s.seconds=seconds;main(p);
            });
        }
        button(m,45,Material.OAK_SIGN,"Ручной ввод",List.of("/cpv time 6h"),()->hint(p,"Введите /cpv time 6h (также: 1d6h)."));
        button(m,49,Material.ARROW,"Назад",List.of(),()->main(p));p.openInventory(m.inventory);
    }
    public void blocks(Player p,int page,String filter) {
        var all=Arrays.stream(Material.values()).filter(Material::isBlock).filter(x->!x.isAir())
            .filter(x->x.name().toLowerCase(Locale.ROOT).contains(filter.toLowerCase(Locale.ROOT))).sorted(Comparator.comparing(Enum::name)).toList();
        int current=Math.max(0,Math.min(page,Math.max(0,(all.size()-1)/45)));
        var m=menu(p,"blocks",current,"Блоки — "+(current+1));
        for(int i=current*45;i<Math.min(all.size(),current*45+45);i++) {
            Material mat=all.get(i);button(m,i%45,mat,mat.name(),List.of("Выбрать исторический Material"),()->{plugin.session(p).block=mat;main(p);});
        }
        pages(m,p,current,all.size(),n->blocks(p,n,filter));
        button(m,50,Material.NETHER_STAR,"Все блоки",List.of("Искать без ограничения по Material","/cpv block all"),()->{plugin.session(p).block=null;main(p);});
        button(m,48,Material.OAK_SIGN,"Поиск по названию",List.of("/cpv blocks chest","Точный выбор: /cpv block redstone_wire"),()->hint(p,"Введите /cpv blocks <часть названия> или /cpv block <material>."));
        p.openInventory(m.inventory);
    }
    private void players(Player p,int page) {
        var names=new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
        Bukkit.getOnlinePlayers().forEach(x->names.add(x.getName()));names.addAll(plugin.knownNames());
        var all=new ArrayList<>(names);int current=Math.max(0,Math.min(page,Math.max(0,(all.size()-1)/45)));
        var m=menu(p,"players",current,"Игроки — "+(current+1));
        for(int i=current*45;i<Math.min(all.size(),current*45+45);i++) {
            String name=all.get(i);button(m,i%45,Material.PLAYER_HEAD,name,List.of("Выбрать"),()->{plugin.session(p).target=name;main(p);});
        }
        pages(m,p,current,all.size(),n->players(p,n));
        button(m,50,Material.NETHER_STAR,"Все игроки",List.of("Вся история онлайн- и офлайн-игроков","Системные записи #fire/#tnt исключаются","/cpv player all"),()->{plugin.session(p).target=null;main(p);});
        button(m,48,Material.OAK_SIGN,"Ввести любой исторический ник",List.of("/cpv player mamont124_1994","Работает и для офлайн-игроков"),()->hint(p,"Введите /cpv player <ник>. Наличие игрока онлайн не требуется."));
        p.openInventory(m.inventory);
    }
    public void results(Player p,int page) {
        if(!plugin.allowed(p,"search"))return;
        var s=plugin.session(p);int current=Math.max(0,Math.min(page,Math.max(0,(s.results.size()-1)/45)));
        var m=menu(p,"results",current,"Результаты — "+(current+1));
        for(int i=current*45;i<Math.min(s.results.size(),current*45+45);i++) {
            int index=i;var r=s.results.get(i);
            button(m,i%45,r.block()==null?Material.PAPER:r.block(),"#"+(i+1)+" "+r.player()+" — "+(r.block()==null?"Контейнер: тип неизвестен":r.block()),
                List.of(plugin.time(r.timestamp()),r.action().name(),r.world()+" "+r.x()+" "+r.y()+" "+r.z(),"Клик: выбрать и телепортироваться"),()->{
                    plugin.navigation().select(p,index);p.closeInventory();plugin.navigation().teleport(p);
                });
        }
        pages(m,p,current,s.results.size(),n->results(p,n));p.openInventory(m.inventory);
    }
    private void pages(Menu m,Player p,int current,int count,java.util.function.IntConsumer open) {
        if(current>0)button(m,45,Material.ARROW,"Предыдущая страница",List.of(),()->open.accept(current-1));
        if((current+1)*45<count)button(m,53,Material.ARROW,"Следующая страница",List.of(),()->open.accept(current+1));
        button(m,49,Material.COMPASS,"Фильтры",List.of("Всего: "+count),()->main(p));
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

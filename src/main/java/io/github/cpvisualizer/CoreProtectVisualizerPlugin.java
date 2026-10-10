package io.github.cpvisualizer;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Level;

public final class CoreProtectVisualizerPlugin extends JavaPlugin implements Listener {
    private ConfigManager settings;
    private SearchManager search;
    private ResultRenderer renderer;
    private GuiManager gui;
    private NavigationManager navigation;
    private final Map<UUID,PlayerSearchSession> sessions=new HashMap<>();
    private final LinkedHashSet<String> knownNames=new LinkedHashSet<>();
    private static final DateTimeFormatter FORMAT=DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");
    @Override public void onEnable() {
        try {
            saveDefaultConfig();settings=ConfigManager.read(getConfig());
            var hook=new CoreProtectHook();search=new SearchManager(this,hook,settings);
            renderer=new VisualizationManager(this);navigation=new NavigationManager(this);gui=new GuiManager(this);
            var commands=new CommandManager(this);var command=Objects.requireNonNull(getCommand("cpv"));
            command.setExecutor(commands);command.setTabCompleter(commands);
            Bukkit.getPluginManager().registerEvents(gui,this);Bukkit.getPluginManager().registerEvents(this,this);
            Bukkit.getScheduler().runTaskTimer(this,renderer::tick,10,10);
            getLogger().info("CoreProtect API hooked successfully. Private visualization ready.");
        } catch(Exception | LinkageError ex) {
            getLogger().log(Level.SEVERE,"Could not enable CPVisualizer. Paper 26.3 and CoreProtect with API 12 enabled are required.",ex);
            Bukkit.getPluginManager().disablePlugin(this);
        }
    }
    @Override public void onDisable() {
        if(search!=null) {sessions.values().forEach(search::cancel);search.close();}
        Bukkit.getScheduler().cancelTasks(this);
        if(gui!=null)gui.closeMenus();if(renderer!=null)renderer.close();sessions.clear();knownNames.clear();
    }
    public void reloadSettings(Player p) {
        try {
            reloadConfig();var replacement=ConfigManager.read(getConfig());
            // Keep the same executor: an API call cannot be forcibly stopped safely.
            boolean restart=replacement.workers()!=settings.workers() || replacement.queue()!=settings.queue();
            sessions.values().forEach(search::cancel);renderer.close();gui.closeMenus();
            sessions.values().forEach(s->{s.results=List.of();s.selected=0;s.seconds=Math.min(s.seconds,replacement.maxTime());s.radius=Math.min(s.radius,replacement.maxRadius());});
            settings=replacement;
            tell(p,"Configuration reloaded; searches and highlights cleared."+(restart?" Queue capacity and worker count take effect after a server restart.":""));
        } catch(Exception ex) {getLogger().log(Level.WARNING,"Invalid CPVisualizer config; previous settings retained.",ex);tell(p,"Invalid config.yml: "+ex.getMessage()+". Previous settings retained.");}
    }
    public void clear(Player p) {var s=session(p);search.cancel(s);renderer.clear(p.getUniqueId());s.results=List.of();s.selected=0;s.visibleUntil=0;}
    @EventHandler public void quit(PlayerQuitEvent e) {var s=sessions.remove(e.getPlayer().getUniqueId());if(s!=null)search.cancel(s);renderer.clear(e.getPlayer().getUniqueId());}
    public boolean allowed(Player p,String permission) {
        if(p.hasPermission("cpvisualizer.use") && p.hasPermission("cpvisualizer."+permission))return true;
        tell(p,"Missing permission: cpvisualizer."+permission);return false;
    }
    public void tell(Player p,String message){p.sendMessage(Component.text("[CPV] ",NamedTextColor.GOLD).append(Component.text(message,NamedTextColor.GRAY)));}
    public String time(long timestamp){return FORMAT.withZone(settings.zone()).format(Instant.ofEpochMilli(timestamp));}
    public String brief(SearchResult r){return r.player()+" | "+(r.block()==null?"Container: unknown type":r.block())+" | "+r.action()+" | "+time(r.timestamp())+" | "+r.world()+" "+r.x()+" "+r.y()+" "+r.z();}
    public PlayerSearchSession session(Player p){return sessions.computeIfAbsent(p.getUniqueId(),id->new PlayerSearchSession(p.getName(),settings.defaultTime()));}
    public Map<UUID,PlayerSearchSession> sessions(){return sessions;}
    public ConfigManager settings(){return settings;}
    public SearchManager search(){return search;}
    public ResultRenderer renderer(){return renderer;}
    public GuiManager gui(){return gui;}
    public NavigationManager navigation(){return navigation;}
    public Set<String> knownNames(){return Collections.unmodifiableSet(knownNames);}
    public void remember(String name){knownNames.add(name);while(knownNames.size()>256)knownNames.remove(knownNames.iterator().next());}
}

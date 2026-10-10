package io.github.cpvisualizer;

import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import java.util.*;

public final class CommandManager implements CommandExecutor,TabCompleter {
    private final CoreProtectVisualizerPlugin plugin;
    private static final List<String> COMMANDS=List.of("search","player","block","blocks","action","time","radius","scope","next","prev","tp","clear","info","results","reload","help");
    public CommandManager(CoreProtectVisualizerPlugin plugin){this.plugin=plugin;}
    @Override public boolean onCommand(CommandSender sender,Command command,String label,String[] args) {
        if(!(sender instanceof Player p)) {sender.sendMessage("CPVisualizer: this command is available in game only.");return true;}
        if(!plugin.allowed(p,"use"))return true;
        if(args.length==0){plugin.gui().main(p);return true;}
        var s=plugin.session(p);String cmd=args[0].toLowerCase(Locale.ROOT);
        try {
            switch(cmd) {
                case "search" -> plugin.search().search(p);
                case "player" -> {String name=argument(args);if(FilterSelection.isAll(name)) {s.target=null;plugin.tell(p,"Players: all (online and offline).");}
                    else {if(!name.matches("[A-Za-z0-9_]{1,16}"))throw new IllegalArgumentException("Player name: 1-16 letters, digits or underscores; all selects every player.");s.target=name;plugin.remember(name);plugin.tell(p,"Player: "+name);}}
                case "block" -> {String name=argument(args);if(FilterSelection.isAll(name)) {s.block=null;plugin.tell(p,"Blocks: all.");}
                    else {var material=Material.matchMaterial(name);if(material==null || !material.isBlock() || material.isAir())throw new IllegalArgumentException("Specify a block material or all.");s.block=material;plugin.tell(p,"Block: "+material);}}
                case "blocks","bloks" -> {if(args.length==2 && FilterSelection.isAll(args[1])) {s.block=null;plugin.tell(p,"Blocks: all.");}
                    else {if(args.length>2)throw new IllegalArgumentException("/cpv blocks [filter/all]");plugin.gui().blocks(p,0,args.length>1?args[1]:"");}}
                case "action" -> {s.action=ActionType.valueOf(argument(args).toUpperCase(Locale.ROOT));plugin.tell(p,"Action: "+s.action);}
                case "time" -> {int time=TimeParser.seconds(argument(args));if(time>plugin.settings().maxTime())throw new IllegalArgumentException("The period exceeds search.max-time.");s.seconds=time;plugin.tell(p,"Period: "+time+" seconds.");}
                case "radius" -> {int radius=Integer.parseInt(argument(args));if(radius<1 || radius>plugin.settings().maxRadius())throw new IllegalArgumentException("Radius must be 1.."+plugin.settings().maxRadius());s.radius=radius;s.scope=SearchQuery.Scope.RADIUS;plugin.tell(p,"X/Z radius: "+radius+"; the center is captured when the search starts.");}
                case "scope" -> {s.scope=SearchQuery.Scope.valueOf(argument(args).toUpperCase(Locale.ROOT));plugin.tell(p,"Scope: "+s.scope);}
                case "next","prev" -> {if(plugin.allowed(p,"search"))plugin.navigation().select(p,s.selected+(cmd.equals("next")?1:-1));}
                case "tp" -> {if(plugin.allowed(p,"search"))plugin.navigation().teleport(p);}
                case "clear" -> {plugin.clear(p);plugin.tell(p,"Search cancelled; results and highlights removed.");}
                case "info" -> {if(plugin.allowed(p,"search")){if(s.current()==null)plugin.tell(p,"No results.");else{plugin.tell(p,plugin.brief(s.current()));plugin.tell(p,s.current().details());}}}
                case "results" -> plugin.gui().results(p,0);
                case "reload" -> {if(plugin.allowed(p,"admin"))plugin.reloadSettings(p);}
                default -> plugin.tell(p,"/cpv [search | player <name/all> | block <material/all> | blocks <filter/all> | action <type> | time <6h> | radius <100> | scope <world/all/radius> | next | prev | tp | info | results | clear | reload]");
            }
        } catch(IllegalArgumentException ex) {plugin.tell(p,"Invalid parameter: "+ex.getMessage());}
        return true;
    }
    private String argument(String[] args) {if(args.length!=2)throw new IllegalArgumentException("The subcommand requires one argument. /cpv help");return args[1];}
    @Override public List<String> onTabComplete(CommandSender sender,Command command,String alias,String[] args) {
        if(!sender.hasPermission("cpvisualizer.use"))return List.of();
        List<String> options=List.of();
        if(args.length==1) options=COMMANDS.stream().filter(x->!x.equals("reload") || sender.hasPermission("cpvisualizer.admin")).toList();
        if(args.length==2) options=switch(args[0].toLowerCase(Locale.ROOT)) {
            case "action" -> Arrays.stream(ActionType.values()).map(Enum::name).toList();
            case "block","blocks","bloks" -> java.util.stream.Stream.concat(java.util.stream.Stream.of("all"),Arrays.stream(Material.values()).filter(Material::isBlock).filter(m->!m.isAir()).map(m->m.name().toLowerCase(Locale.ROOT))).toList();
            case "time" -> List.of("15m","1h","6h","12h","24h","3d","7d","30d");
            case "scope" -> List.of("world","all","radius");
            case "radius" -> List.of("50","100","500","1000","5000");
            case "player" -> {var names=new TreeSet<>(plugin.knownNames());Bukkit.getOnlinePlayers().forEach(p->names.add(p.getName()));var all=new ArrayList<String>();all.add("all");names.stream().filter(n->!n.equalsIgnoreCase("all")).forEach(all::add);yield all;}
            default -> List.of();
        };
        String prefix=args.length==0?"":args[args.length-1].toLowerCase(Locale.ROOT);
        return options.stream().filter(x->x.toLowerCase(Locale.ROOT).startsWith(prefix)).limit(100).toList();
    }
}

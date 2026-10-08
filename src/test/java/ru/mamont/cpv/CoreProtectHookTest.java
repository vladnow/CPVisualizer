package ru.mamont.cpv;

import net.coreprotect.CoreProtectAPI;
import net.coreprotect.api.LookupOptions;
import net.coreprotect.api.result.ContainerResult;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import java.util.*;
import java.util.concurrent.CancellationException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CoreProtectHookTest {
    private final CoreProtectAPI api = mock(CoreProtectAPI.class);
    private CoreProtectHook hook() { when(api.isEnabled()).thenReturn(true); return new CoreProtectHook(api); }
    private ConfigManager settings(int limit) {
        var yaml = new YamlConfiguration(); yaml.set("search.max-results",limit); return ConfigManager.read(yaml);
    }
    private World world(String name) { var world = mock(World.class); when(world.getName()).thenReturn(name); return world; }
    private SearchQuery query(String player,Material block,ActionType action,SearchQuery.Scope scope,Map<String,World> worlds,int limit) {
        World world = worlds.get("world");
        return new SearchQuery(player,block,action,3600,scope,100,new Location(world,143,64,-502),"world",worlds,settings(limit));
    }
    private String[] row(String id,String player,String world,long time,Material material) {
        String[] raw = {id}; var parsed = mock(CoreProtectAPI.ParseResult.class);
        when(parsed.getPlayer()).thenReturn(player); when(parsed.worldName()).thenReturn(world);
        when(parsed.getTimestamp()).thenReturn(time); when(parsed.getType()).thenReturn(material);
        when(parsed.getActionId()).thenReturn(1); when(api.parseResult(raw)).thenReturn(parsed);return raw;
    }
    @Test void globalWorldUsesPositiveRadiusAndOmitsMaterialRestriction() {
        var hook = hook(); var world = world("world");
        when(api.performPartialLookup(anyInt(),anyList(),isNull(),isNull(),isNull(),anyList(),anyInt(),any(),anyInt(),anyInt())).thenReturn(List.of());
        var q = query(null,null,ActionType.PLACE,SearchQuery.Scope.WORLD,Map.of("world",world),500);
        hook.lookup(q,()->{});
        var center = ArgumentCaptor.forClass(Location.class);
        verify(api).performPartialLookup(eq(3600),eq(List.of("#global")),isNull(),isNull(),isNull(),eq(List.of(1)),
            eq(BlockLookupPlan.WORLD_RADIUS),center.capture(),eq(0),eq(2001));
        assertSame(world,center.getValue().getWorld());assertEquals(0,center.getValue().getX());assertEquals(0,center.getValue().getZ());
        assertEquals(143,q.center().getX()); // Planning never moves the captured administrator location.
    }
    @Test void globalAllWorldsMergesNewestPlayerEventsAndHonorsSingleResultLimit() {
        var hook = hook(); var world = world("world"); var nether = world("nether");
        String[] older = row("old","Alex","nether",1000,Material.CHEST);
        String[] recent = row("recent","Steve","world",3000,Material.STONE);
        String[] newest = row("newest","Alex","world",4000,Material.BARREL);
        String[] system = row("system","#fire","world",5000,Material.STONE);
        when(api.performPartialLookup(anyInt(),anyList(),isNull(),isNull(),isNull(),anyList(),anyInt(),any(),anyInt(),anyInt()))
            .thenAnswer(call->{ Location center=call.getArgument(7); return center.getWorld()==nether ? Collections.singletonList(older) : List.of(system,recent,newest); });
        var result = hook.lookup(query(null,null,ActionType.PLACE,SearchQuery.Scope.ALL,Map.of("world",world,"nether",nether),2),()->{});
        assertEquals(List.of(4000L,3000L),result.results().stream().map(SearchResult::timestamp).toList());
        assertTrue(result.limited());assertTrue(result.results().stream().noneMatch(r->r.player().startsWith("#")));
        verify(api,times(2)).performPartialLookup(anyInt(),eq(List.of("#global")),isNull(),isNull(),isNull(),anyList(),eq(BlockLookupPlan.WORLD_RADIUS),any(),eq(0),eq(2001));
    }
    @Test void globalRadiusRetainsAdministratorCenterAndSelectedMaterial() {
        var hook=hook();var world=world("world");
        when(api.performPartialLookup(anyInt(),anyList(),isNull(),anyList(),isNull(),anyList(),anyInt(),any(),anyInt(),anyInt())).thenReturn(List.of());
        hook.lookup(query(null,Material.CHEST,ActionType.INTERACT,SearchQuery.Scope.RADIUS,Map.of("world",world),500),()->{});
        var center=ArgumentCaptor.forClass(Location.class);
        verify(api).performPartialLookup(eq(3600),eq(List.of("#global")),isNull(),eq(List.of(Material.CHEST)),isNull(),eq(List.of(2)),eq(100),center.capture(),eq(0),eq(2001));
        assertEquals(143,center.getValue().getX());assertEquals(-502,center.getValue().getZ());
    }
    @Test void singlePlayerAllBlocksPreservesUnboundedWorldScopeAndActionFilter() {
        var hook=hook();
        when(api.performPartialLookup(anyInt(),anyList(),isNull(),isNull(),isNull(),anyList(),anyInt(),isNull(),anyInt(),anyInt())).thenReturn(List.of());
        hook.lookup(query("Alex",null,ActionType.BREAK,SearchQuery.Scope.ALL,Map.of(),500),()->{});
        verify(api).performPartialLookup(eq(3600),eq(List.of("Alex")),isNull(),isNull(),isNull(),eq(List.of(0)),eq(-1),isNull(),eq(0),eq(501));
    }
    @Test void allBlocksKeepsUnknownContainerWithoutMislabelingItemAsBlock() {
        var hook=hook();var row=mock(ContainerResult.class);
        when(row.getPlayer()).thenReturn("Alex");when(row.worldName()).thenReturn("archived_world");when(row.getActionId()).thenReturn(1);
        when(row.getTimestamp()).thenReturn(4000L);when(row.getType()).thenReturn(Material.DIAMOND);when(row.getAmount()).thenReturn(3);
        when(api.containerLookup(any(LookupOptions.class))).thenReturn(List.of(row));
        var result=hook.lookup(query(null,null,ActionType.CONTAINER_ADD,SearchQuery.Scope.ALL,Map.of(),500),()->{});
        assertEquals(1,result.results().size());assertNull(result.results().getFirst().block());
        assertTrue(result.results().getFirst().details().contains("DIAMOND ×3"));assertEquals(1,result.unresolved());
        var options=ArgumentCaptor.forClass(LookupOptions.class);verify(api).containerLookup(options.capture());
        assertNull(options.getValue().getUser());assertEquals(2001,options.getValue().getLimitCount());
        var specific=hook.lookup(query(null,Material.CHEST,ActionType.CONTAINER_ADD,SearchQuery.Scope.ALL,Map.of(),500),()->{});
        assertTrue(specific.results().isEmpty()); // Unknown type never matches a specific requested container.
    }
    @Test void cancellationStopsBeforeDatabaseLookup() {
        var hook=hook();
        assertThrows(CancellationException.class,()->hook.lookup(query(null,null,ActionType.ALL,SearchQuery.Scope.WORLD,Map.of(),500),()->{throw new CancellationException();}));
        verify(api,never()).containerLookup(any(LookupOptions.class));
        verify(api,never()).performPartialLookup(anyInt(),anyList(),any(),any(),any(),anyList(),anyInt(),any(),anyInt(),anyInt());
    }
}

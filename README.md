# CPVisualizer

**Private CoreProtect history visualization for Paper 26.3 and Java 25.**

CPVisualizer is a server-side administration plugin. Search recorded block and
container events, browse them in an inventory GUI, and highlight matching
positions for the administrator running the search. Each administrator has an
independent session. No client mod or ProtocolLib is required.

The plugin reads history through the public CoreProtect API. It does not change
real blocks, roll back events, or modify CoreProtect's configuration or database.

Version **1.1.1** targets Paper 26.3 and Java 25. Menus, messages, and documentation are in English.
Download the plugin and its source archive from [GitHub Releases](../../releases/latest).
Version 1.1.0 remains available for Paper 26.2.

## Requirements and installation

| Requirement | Version |
|---|---|
| Server | Paper 26.3 |
| Java | 25 |
| Required plugin | CoreProtect with Paper 26.3 compatibility and API 12 enabled |

1. Stop the server.
2. Install CoreProtect separately, or keep the existing compatible installation.
3. Take `CPVisualizer-1.1.1-paper26.3.jar` from the Paper 26.3 release bundle.
4. Place the JAR in the server's `plugins/` directory alongside CoreProtect.
5. Start the server with Java 25 and run `/cpv` in game as an operator.

CoreProtect is a required dependency and is **not bundled inside** CPVisualizer.
CPVisualizer disables itself if the dependency or its API is unavailable.


## Quick start: all players and all blocks

```text
/cpv player all
/cpv block all
/cpv action ALL
/cpv time 24h
/cpv radius 100
/cpv search
```

This searches recorded actions by online and offline players involving any block
type during the last 24 hours, within an inclusive X/Z square extending 100 blocks
from the administrator in each direction. The radius includes all heights, rather
than defining a sphere. The search center is captured when `/cpv search` runs.

`/cpv blocks all`, `/cpv bloks all`, and `*` instead of `all` also clear the block
filter. `/cpv player *` clears the player filter. The player and block selectors
have corresponding GUI buttons. Selecting a particular player or material
restores that filter. Action, time, and scope filters remain independent.

CoreProtect system actors whose names begin with `#`, such as `#fire`, `#tnt`, and
`#hopper`, are excluded from all-player searches. `all` is a reserved selector,
not a literal player name.

To investigate one player's container additions, replace `ExamplePlayer` with
the historical player name:

```text
/cpv player ExamplePlayer
/cpv block chest
/cpv action CONTAINER_ADD
/cpv time 6h
/cpv scope world
/cpv search
```

The player does not have to be online or present in Paper's local player cache.

## Commands

Commands are available in game. `/cpv help` displays command usage.

| Command | Description |
|---|---|
| `/cpv` | Open the filter menu. |
| `/cpv player <name>` | Select an online or historical offline player name. |
| `/cpv player all` | Search all players in the recorded history. |
| `/cpv block <material>` | Select an exact block material, such as `chest` or `redstone_wire`. |
| `/cpv block all`, `/cpv blocks all` | Search every block type. |
| `/cpv blocks [text]` | Browse and filter the paginated material selector. |
| `/cpv action <type>` | Select `PLACE`, `BREAK`, `INTERACT`, `CONTAINER_ADD`, `CONTAINER_REMOVE`, or `ALL`. |
| `/cpv time <duration>` | Set a period, such as `30s`, `15m`, `6h`, `3d`, or `1d6h`. |
| `/cpv scope world` | Search the world occupied when the search starts. |
| `/cpv scope all` | Search across worlds; see the loaded-world restriction below. |
| `/cpv radius <number>` | Set the radius and switch to radius scope. |
| `/cpv scope radius` | Return to the previously selected radius. |
| `/cpv search` | Start a search and replace the previous request in this session. |
| `/cpv results` | Open saved results, with 45 events per page. |
| `/cpv next`, `/cpv prev` | Select the next or previous event, wrapping at either end. |
| `/cpv tp` | Teleport to the selected event if permitted. |
| `/cpv info` | Display details of the selected event. |
| `/cpv clear` | Cancel the search and remove saved results and highlights. |
| `/cpv reload` | Validate and apply configuration, clearing searches and highlights. |

Changing filters affects the **next** search; it does not replace the current
saved results. The GUI provides time presets, player and block selectors, and
event details including time, world, coordinates, player, material, and action.
Clicking a result selects it and requests teleportation if the administrator has
the required permission.

The player menu lists online players, up to 256 names remembered during the
current server run, and names from search results. It does not scan CoreProtect's
entire user directory. Use `/cpv player <name>` for other historical players.

## Permissions

All permissions default to operators. Grant individual permissions through a
permission manager to delegate access.

| Permission | Access |
|---|---|
| `cpvisualizer.use` | Use `/cpv` and its filter menus. |
| `cpvisualizer.search` | Search, view results, and select events. |
| `cpvisualizer.teleport` | Teleport to events; normal use also requires search access. |
| `cpvisualizer.admin` | Includes the permissions above and allows configuration reloads. |

## Highlights and navigation

Highlights are temporary white stained-glass `BlockDisplay` entities with
age-colored glowing outlines. The selected position receives stronger emphasis.
They are hidden by default before being added to the world and shown only to the
administrator who owns them, including when other players join later.

Default behavior:

- Show up to 100 unique positions within 96 blocks of the administrator.
- Render only in already loaded chunks of the current world.
- Refresh every 10 ticks, creating at most 10 highlights per administrator per cycle.
- Keep highlights for 300 seconds after a search or event selection.
- Show nearby or targeted event details in the action bar.
- Display timestamps in the configurable `display-timezone`, initially `Europe/Moscow`.

Multiple events at one position remain separate GUI entries. A shared highlight
uses the selected event, or otherwise the newest event at that position.
Selecting another event restarts the display lifetime. Highlights are removed
on clear, logout, plugin shutdown, configuration reload, expiry, movement out of
range, or loss of permissions. They are not persistent world entities.

Glass is partially transparent, but `BlockDisplay` does not provide arbitrary
alpha transparency. Appearance depends on client rendering settings and shaders.
Normal entity tracking and view-distance limits still apply.

Teleportation asynchronously loads an existing destination chunk without
generating one. Spectators move above the event position. Other game modes require
an available location with solid support nearby; otherwise teleportation is
declined. The plugin checks permissions, world availability, height, and world
border before teleporting. It does not change the player's game mode or protect
against every environmental hazard afterward.

## Search coverage and limits

### Worlds and radius

For all-player **block** searches, `world` searches the current world and `all`
merges searches across the worlds loaded when the request starts. **Unloaded
worlds are excluded from that all-player block search**, and a notice is shown
afterward. A specific player's block search with `all` can query available
CoreProtect history from unloaded worlds. Searches do not load or generate chunks.

CoreProtect API 12 requires a positive radius for a global block lookup. Global
all-player searches therefore use a radius of 30,000,000 around X=0, Z=0 in each
applicable world, covering legal vanilla coordinates independently of its world
border. Normal radius searches use the administrator's captured position.

### Recorded actions and containers

Only events logged and retained by CoreProtect can be found. `INTERACT` means
CoreProtect's recorded interaction action, not every possible Bukkit interaction.

Container transaction material refers to the **item**, rather than the container
block. CPVisualizer infers the historical container type from earlier block
events at the same world and position, considering subsequent destruction.
It does not identify a past container by reading the current block.

API 12 container lookups support user, time, radius, and row-limit options.
World and transaction-action filters are applied afterward. Historical evidence
uses bounded block lookups at each transaction position.

For a specific container filter such as `CHEST` or `BARREL`, events without
sufficient historical evidence are excluded. With all blocks selected, those
transactions remain visible with an unknown container type; the item type appears
only in additional details. Rolled-back events, incomplete logs, movable
containers, and same-second changes can make historical inference uncertain.
Cross-check the original CoreProtect history when an investigation requires
certainty.

### Bounded results

| Setting | Default | Purpose |
|---|---|---|
| `search.max-results` | 500 | Maximum events in the final merged results. |
| `search.block-scan-limit` | 2000 | Rows read per world or radius area for all-player block searches before system actors are removed. |
| `search.container-scan-limit` | 2000 | Maximum container transactions read before additional filtering. |
| `search.history-position-limit` | 128 | Maximum positions checked for historical container evidence. |
| `search.history-row-limit` | 1000 | Maximum historical rows examined per position. |
| `search.max-time` | `30d` | Maximum selectable search period. |
| `search.max-radius` | 5000 | Maximum user-selected radius. |

Specific-player block lookups are limited directly to `search.max-results`.
For action `ALL`, matching block and container events are merged, sorted newest
first, and trimmed to the final result limit. GUI pagination uses this saved
selection and does not issue another query per page.

A limit notice means the result may be incomplete. Additional filtering can
produce fewer results, including zero, even after the scan limit is reached.
Narrow the player, period, or radius before concluding that an action did not
occur. CoreProtect may log a database error and return an empty response; its
public API does not always distinguish that response from no matching history.

## Configuration and resource use

Configuration is created at `plugins/CPVisualizer/config.yml` on first startup.
It controls search limits, time presets, colors, visibility distance, lifetime,
and timestamp timezone.

History queries run in a bounded worker pool: two concurrent queries, eight
queued requests, a 1000 ms cooldown, and a 30-second user-facing timeout by
default. GUI updates, entity rendering, and teleportation run on Paper's main
thread. A new search cancels delivery of the previous response and removes a
queued predecessor where possible.

CoreProtect does not expose JDBC query cancellation. A timeout discards late
results; an already running API call occupies its worker until it returns.
Restart the server to change pool size or queue capacity.

Age thresholds must increase strictly. Supported colors include `GREEN`,
`YELLOW`, `GOLD`/`ORANGE`, `RED`, `BLUE`, `WHITE`, `AQUA`, `PURPLE`, and quoted
hex colors such as `'#RRGGBB'`. Invalid startup configuration disables the plugin.
A failed reload retains the previous valid settings.

## Building from source

Install JDK 25 and set `JAVA_HOME`.

Windows:

```powershell
.\gradlew.bat clean build
```

Linux or macOS:

```sh
sh gradlew clean build
```

The build uses Gradle Wrapper 9.1.0 and Paper API `26.3.build.169-beta`.
Without a local CoreProtect JAR, Gradle downloads
`net.coreprotect:coreprotect:24.1` from the official PlayPro Maven repository.
Optionally place a compatible JAR at `libs/CoreProtect-24.1.jar` to compile
against that local dependency. To force the Maven dependency on Windows:

```powershell
.\gradlew.bat clean build -PcoreProtectFromMaven
```

To verify against the separately built 26.3 port, pass
`-PcoreProtectJar=<path-to-CoreProtect-24.1-paper26.3.1.jar>`.

Output: `build/libs/CPVisualizer.jar`. Paper and CoreProtect are compile-only
dependencies and are not bundled. JUnit and Mockito are used only for tests.
The first build needs network access.

If Gradle's Windows test worker fails with `GradleWorkerMain` under a non-ASCII
project or cache path, use ASCII-only paths for the checkout and
`GRADLE_USER_HOME`. This build-path issue does not affect the installed JAR.

## Validation

Version 1.1.1 passed all 14 automated tests against the modified CoreProtect
24.1 build and Paper API `26.3.build.169-beta`. Both plugins loaded together on
an actual Paper 26.3 build 169 server. Real SQLite history was read through the
visualizer, and CoreProtect rollback/restore operations were checked on test
blocks. Private BlockDisplay creation was checked on the server. **Visual
verification of the GUI and private markers with two real clients remains
outstanding.** MySQL and WorldEdit integration were not exercised.
See [TESTING.md](TESTING.md) for the server verification checklist.

## Reference documentation

- [Paper project setup](https://docs.papermc.io/paper/dev/project-setup/)
- [Paper 26.3 API](https://jd.papermc.io/paper/26.3/)
- [CoreProtect 24.1 release](https://github.com/PlayPro/CoreProtect/releases/tag/v24.1)
- [CoreProtect API 12](https://docs.coreprotect.net/api/version/v12/)
- [CoreProtect API source at v24.1](https://github.com/PlayPro/CoreProtect/blob/v24.1/src/main/java/net/coreprotect/CoreProtectAPI.java)

The implementation targets API 12 and does not rely on API 13-only lookup
options. CPVisualizer's implementation is independent; CoreProtect's
implementation is not copied into this project.

## License

CPVisualizer is distributed under the [MIT License](LICENSE). CoreProtect is a separate dependency with its own license.

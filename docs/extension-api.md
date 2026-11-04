# SkGame — Native extension API

Status: **planned** (loader is Phase 1 of [standalone-migration.md](standalone-migration.md))

A **native extension** is a jar implementing `cz.nox.skgame.api.module.SkGameModule`,
loaded by SkGame itself (not by Bukkit). Extensions work in **both** addon and
standalone mode — unlike `.sk` scripts, which only work in addon mode.

Reference implementation: **Bridge Builders**
(`F:\Projects\Minecraft\Plugins\BridgeBuilders`).

---

## Loader contract (SkGame side — Phase 1)

### Discovery

On enable, after built-in modules, SkGame scans:

```
plugins/SkGame/modules/*.jar
```

### Descriptor

Each external jar must contain `skgame-extension.yml` at its root (named distinctly
from Bukkit's `plugin.yml`, since an extension is **not** a Bukkit plugin):

```yaml
id: bridge_builders                              # unique, lowercase; == SkGameModule.getId()
name: Bridge Builders
main: cz.nox.bridgebuilders.BridgeBuildersModule # FQN implementing SkGameModule, public no-arg ctor
api-version: 1                                    # SkGame extension API major this targets
dependencies: []                                 # other module ids required first
authors: [ vesely-jiri ]
version: 1.0.0
```

### Classloading

```java
URL jarUrl = jar.toURI().toURL();
URLClassLoader cl = new URLClassLoader(new URL[]{ jarUrl }, SkGame.class.getClassLoader());
Class<?> main = Class.forName(descriptor.main, true, cl);
SkGameModule module = (SkGameModule) main.getDeclaredConstructor().newInstance();
```

- Parent **must** be SkGame's classloader so `api` types (`MiniGame`, `Session`,
  events) resolve to the same `Class` objects — `instanceof`/casts across the
  boundary must work.
- The extension sees: SkGame `api` + `core`, Paper API, and (addon mode) Skript.
- The extension is responsible for shading/relocating its own third-party libs.
- Keep a `Map<String, URLClassLoader>` to close loaders on disable/reload.

### Lifecycle (identical to built-in modules)

```
canEnable(plugin)                      -> false = skip, log reason
ResourceInstaller.installAll(...)      -> install getResourcePaths()
onEnable(plugin)                       -> register minigame + listeners
  if ADDON: register getSkriptClasses()
...
onDisable(plugin)  (reverse order)     -> unregister, then close classloader
```

Config gate: `modules.<id>.enabled` in SkGame `config.yml` (`isEnabledByDefault()`
decides the default when the key is absent) — same as built-ins.

---

## Authoring a native minigame (extension side)

A minigame is **data + behaviour**:

- **Data**: a `MiniGame` registered with `MiniGameManager`, declaring icon, players,
  tags, teams, and **gamemap value defs** (per-map admin-configured values — spawns,
  regions, goal locations). Admins edit these in SkGame's existing maps GUI.
- **Behaviour**: a Bukkit `Listener` reacting to SkGame game events
  (`GameStartEvent`, `GameStopEvent`, …) + vanilla events, gated to your minigame id.

### 1. Implement `SkGameModule`

```java
public final class BridgeBuildersModule implements SkGameModule {
    @Override public String getId() { return "bridge_builders"; }
    @Override public String getName() { return "Bridge Builders"; }
    @Override public boolean canEnable(SkGame plugin) { return true; }

    @Override public void onEnable(SkGame plugin) {
        BridgeBuildersGame.register();                 // register the MiniGame data
        Bukkit.getPluginManager().registerEvents(new BridgeBuildersListener(), plugin);
    }
}
```

### 2. Register the `MiniGame` (mirrors the `.sk` `register minigame` block)

Keys map 1:1 to what `MiniGameEntryHelper.apply(...)` sets from Skript:

| `.sk` entry        | Java call                                            |
|--------------------|------------------------------------------------------|
| `name:`            | `mg.setValue("name", "&bBridge Builders")`           |
| `icon:`            | `mg.setValue("icon", new ItemStack(Material.BRIDGE…))`|
| `description:`     | `mg.setValue("description", "&7- …")`                |
| `author:`          | `mg.setValue("author", "SkGame")`                    |
| `min players:`     | `mg.setValue("min_players", 2L)`  // Long            |
| `max players:`     | `mg.setValue("max_players", 8L)`                     |
| `tags:`            | `mg.setTags(EnumSet.of(MinigameTag.TEAM, MinigameTag.PVP))` |
| `team assignment:` | `mg.setTeamAssignment(TeamAssignmentMode.…)`         |
| `teams:`           | `mg.setTeamEntries(List.of(new TeamEntry("red", "&cRed", icon), …))` |
| `cancel events:`   | `mg.setCancelledEvents(EnumSet.of(CancellableEventType.HUNGER, …))` |
| `time:` / `weather:`| `mg.setPlayerTime(0L)` / `mg.setPlayerWeather("clear")` |
| `instructions:`    | `mg.setInstructions(List.of("…"))`                   |
| gamemap value def  | `mg.setGameMapValueDef(key, customValue)`            |
| session value def  | `mg.setSessionValueDef(key, customValue)`            |

```java
MiniGame mg = MiniGameManager.getInstance().registerMiniGame("bridge_builders");
mg.setValue("name", "&bBridge Builders");
mg.setValue("min_players", 2L);
mg.setTeamAssignment(TeamAssignmentMode./* pick */ …);
mg.setTeamEntries(List.of(
    new TeamEntry("red",  "&cRed",  new ItemStack(Material.RED_WOOL)),
    new TeamEntry("blue", "&9Blue", new ItemStack(Material.BLUE_WOOL))));
mg.setGameMapValueDef("red_spawn",  locDef("Red spawn"));
mg.setGameMapValueDef("blue_spawn", locDef("Blue spawn"));
mg.setGameMapValueDef("red_goal",   regionDef("Red goal region"));
mg.setGameMapValueDef("blue_goal",  regionDef("Blue goal region"));
mg.setGameMapValueDef("build_area", regionDef("Buildable area"));
MiniGameManager.getInstance().save();

// Helper — native, Skript-free typing (see §3):
static CustomValue locDef(String name) {
    CustomValue cv = new CustomValue(name);
    cv.setValueType(ValueType.LOCATION);
    return cv;
}
static CustomValue regionDef(String name) {
    CustomValue cv = new CustomValue(name);
    cv.setValueType(ValueType.REGION);
    return cv;
}
```

### 3. `CustomValue` types — native `ValueType` (Phase 2, done)

Type value defs with the native, Skript-free token:

```java
CustomValue cv = new CustomValue("Red spawn");
cv.setValueType(ValueType.LOCATION);   // import cz.nox.skgame.api.game.model.type.ValueType
```

`ValueType` constants: `TEXT, NUMBER, BOOLEAN, LOCATION, BLOCK, ITEM, REGION, PLAYER, TIMESPAN`.

- Native setter is **`setValueType(ValueType)`** and getter **`getValueType()`**. These are the
  only type methods an extension calls.
- The legacy Skript getter/setter is **`getType()` / `setSkriptType(ClassInfo<?>)`** — addon-only,
  wired through a bridge, **never call these from an extension**. They are deliberately named
  differently from `setValueType` so a Skript-free extension compiles: a single overloaded
  `setType(…)` would force `javac` to load `ch.njol.skript.classes.ClassInfo` to resolve the call.
- Plurality, default, bounds, allowed-values are all native: `setPlurality(CustomValuePlurality.PLURAL)`,
  `setDefaultValue(5L)`, `setMinValue/ setMaxValue`, `setAllowedValues(List.of(...))`.

> Bridge Builders must not import `ch.njol.skript.*`. If a capability is only reachable through
> Skript types, raise it as an API gap instead of importing Skript — that would break standalone mode.
> (`TIMESPAN` is the one type still backed by a Skript object at runtime; avoid it in standalone.)

### 4. Game behaviour — read gamemap values at runtime

The `.sk` pattern `gamemap values "spawnpoints" of event-session` maps to reading
the session's selected `GameMap` and pulling the configured value by key
(see `GameMap` / `Session` getters — confirm exact method names against
`api/game/model/GameMap.java` + `Session.java` when implementing).

Lifecycle events to listen for (`api/game/event/`):
`GameStartEvent`, `GameStopEvent` (carries reason string, e.g. `"WIN"`/`"TIE"`),
`GamePlayerSessionJoin`/`Leave`, `PlayerScoreChangeEvent`, `SpectatorJoinEvent`.

End a game: `SessionLifecycleManager.endGame(session, "WIN")` /
make a player a spectator / `startGame(session, reason, delay)`.

---

## Build setup for an extension

Mirror SkGame's build (`build.gradle.kts`): Java 25, Paper `26.2`, shadow plugin,
`compileOnly` SkGame as the API provider. See the Bridge Builders `build.gradle.kts`.

```kotlin
compileOnly("io.papermc.paper:paper-api:26.2.build.124-stable")
compileOnly(files("F:/Projects/SkGame-build/libs/SkGame.jar"))   // or a published artifact
// Skript is NOT a dependency — a native extension must not use Skript types.
```

Package `skgame-extension.yml` into the jar root (via `processResources`), shade your
own libs, and drop the jar into `plugins/SkGame/modules/`.

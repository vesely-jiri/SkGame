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
config: config.yml                               # optional; jar-resource template for modules/<id>.yml (see "Module config")
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

### 5. Module config

Each external module gets an auto-generated, editable config file at
**`plugins/SkGame/modules/<id>.yml`** — the **same folder as the jars**, where `<id>` is
`SkGameModule.getId()`.

**Template** (what the file is seeded with on first load):
- Default: SkGame copies **`config.yml` from your jar root**.
- Override: declare a different jar-resource path in `skgame-extension.yml`:
  ```yaml
  config: configs/bridge_builders.yml   # optional; defaults to "config.yml"
  ```
- If the jar has no such resource, SkGame writes a short generated header so the file still
  appears and is editable.

**Generation rules:** created only if `<id>.yml` is absent; an existing file is **never
overwritten** (user edits + comments preserved). The loaded config is never written back, so
on-disk comments survive.

**Read it** from your module's `onEnable` (the `SkGame plugin` is passed in):
```java
@Override public void onEnable(SkGame plugin) {
    YamlConfiguration cfg = plugin.getModuleConfig(getId());   // org.bukkit.configuration.file.YamlConfiguration
    int teams     = cfg.getInt("teams", 4);
    int perMap    = cfg.getInt("resources-per-map", 64);
    int timerSecs = cfg.getInt("timer-seconds", 300);
}
```
- **API:** `org.bukkit.configuration.file.YamlConfiguration SkGame.getModuleConfig(String moduleId)`.
- Returns the loaded config for a loaded external module; **`null`** if `moduleId` isn't a loaded
  external extension (built-in modules use SkGame's own `config.yml`). Always use `getX(key, default)`
  so missing keys are safe (there is no automatic default-merge; ship every key in your template).

**Reload:** `/skgame reload` (or `/skgame reload config`) re-reads every `<id>.yml` from disk
**into the same `YamlConfiguration` object**, so a module that cached the reference from
`getModuleConfig(...)` sees the new values — no restart, no server rejoin. It never regenerates or
overwrites the file. (If your module caches individual values rather than the config object, re-read
them when you want reload to take effect.)

**Packaging:** put `config.yml` at your jar root (e.g. `src/main/resources/config.yml`, bundled by
`processResources`) with comments and every default key.

### 6. Kits & match settings (lobby GUIs)

A minigame can declare selectable **kits** and host-toggleable **match settings**; SkGame surfaces
both in its existing lobby GUIs. Both models are Skript-free and serialize with the minigame.

**Models** (`api/game/model/`):
```java
KitOption(String id, @Nullable String displayName, @Nullable ItemStack icon)    // displayName: legacy & codes ok
SessionSetting(String key, @Nullable String displayName, boolean defaultValue)  // boolean-only
```
**MiniGame setters:** `mg.setKits(List<KitOption>)`, `mg.setSettings(List<SessionSetting>)`
(getters `getKits()` / `getSettings()`; empty by default).

```java
mg.setKits(List.of(
    new KitOption("miner",   "&eMiner",   new ItemStack(Material.IRON_PICKAXE)),
    new KitOption("axe",     "&cAxe",     new ItemStack(Material.IRON_AXE)),
    new KitOption("furnace", "&6Furnace", new ItemStack(Material.FURNACE))));
mg.setSettings(List.of(new SessionSetting("bb_pvp", "PvP", false)));
```

**Kits — team picker:** when `getKits()` is non-empty, the team-picker GUI gains **one extra row
below the teams** with a button per kit (icon + name, ✔ on the selected one). Clicking (only in
`PREPARATION`, like team pick) stores the choice as a **temporary** session value:
- key **`"kit." + player.getUniqueId()`** → the chosen **kit id** (String), `isTemporary = true`.
- Read it on game start: `session.getValue("kit." + uuid, true)`. **Null = never picked → default to
  the first kit** (`getKits().get(0)`), which is also what the GUI shows pre-selected.

**Settings — session GUI:** when `getSettings()` is non-empty, the session GUI shows a host-only
**"Match Settings"** button at **slot 18** that opens a sub-GUI (the main GUI's control slots around
the minigame slot were full; this sits in the host-config cluster). Each setting is a toggle
(LIME = on / GRAY = off, state in the lore). Host toggles write a **persistent** session value:
- `session.setValue(key, boolean, false)` (`isTemporary = false`); fires
  `SessionSettingsChangedEvent(session, "setting:" + key)`.
- Read it on game start: `session.getValue(key, false)` → a `Boolean`, or **null if never toggled →
  treat as the declared `defaultValue`**. The GUI shows `defaultValue` until first toggled.

Both lists persist in minigames.yml via the YAML-safe `ValueSerialization` path (kit icons stored as
their `ItemStack` `==` form).

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

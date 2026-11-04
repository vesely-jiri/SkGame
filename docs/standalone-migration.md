# SkGame — Standalone / Addon dual-mode migration

Status: **planned** · Owner: @vesely-jiri · Target: SkGame 1.1.0

## Goal

SkGame currently runs **only** as a Skript addon (`depend: [Skript]`). We want two
runtime states, decided automatically at enable time:

| Mode          | Skript present? | Accepts `.sk` scripts | Accepts native extensions |
|---------------|-----------------|-----------------------|---------------------------|
| **Addon**     | yes             | ✅ (full Skript syntax) | ✅                        |
| **Standalone**| no              | ❌                     | ✅                        |

"Native extensions" = jars implementing `SkGameModule`, loaded by SkGame itself
(see [extension-api.md](extension-api.md)). The first native extension is
**Bridge Builders** (`F:\Projects\Minecraft\Plugins\BridgeBuilders`).

The point: a server owner who does not use Skript can still run SkGame + native
minigames. A server owner who uses Skript gets everything — scripts **and** native
extensions.

---

## Current architecture (as of `main`)

Clean layering already exists:

- `cz.nox.skgame.api.*` — public model/events/services (the extension surface)
- `cz.nox.skgame.core.*` — implementation (managers are singletons: `MiniGameManager.getInstance()`, …)
- `cz.nox.skgame.skript.*` — Skript syntax layer: 113 expressions, 28 effects,
  20 events, 13 sections, 11 conditions. **This whole package is addon-only.**
- `cz.nox.skgame.core.module.*` — module system: `SkGameModule` interface,
  `ModuleRegistry.BUILTIN_MODULES`, `ResourceInstaller`.

Built-in modules today: `GuiModule`, `AdminModule`, `DefaultMinigamesModule`,
`MessagesModule`, `DatabaseModule`, `ScoreboardModule` — all registered in-code.
Minigames ship as `.sk` scripts installed by `DefaultMinigamesModule`.

`SkGameModule` already has the right hooks for dual-mode:
- `getSkriptClasses()` → only registered when Skript is present
- `getResourcePaths()` + `resolveResourceTarget()` → resource install

So the module API anticipated this. The work is (1) make SkGame itself boot
without Skript, (2) decouple the core model from Skript, (3) load external module jars.

---

## Skript coupling that blocks standalone (audit)

These are the hard leaks that must be fixed or guarded:

1. **`api/module/SkGameModule.java`** imports `ch.njol.skript.Skript`; the default
   `resolveResourceTarget()` calls `Skript.getInstance().getScriptsFolder()`.
   → NoClassDefFoundError path in standalone if a module ships `scripts/` resources.

2. **`api/game/model/CustomValue.java`** — `type` field is
   `ch.njol.skript.classes.ClassInfo<?>`; `serialize()`/`deserialize()` call
   `ch.njol.skript.registrations.Classes`. `CustomValue` is used by `MiniGame`
   (core model). → The core minigame model cannot (de)serialize value defaults,
   and value *types* cannot be expressed, without Skript on the classpath.
   **This is the biggest blocker.**

3. **`core/module/ResourceInstaller.java`** — `resolveScriptsTarget()` uses
   `Skript.getInstance()`. Core `scripts/` resources are Skript syntax anyway.

4. **`SkGame.java`** (main) — holds `ch.njol.skript.SkriptAddon addon`,
   unconditionally builds the addon, calls `loadAllSkriptClasses(...)`,
   `cleanupLegacyScripts()`, `doReloadScripts()`. All must become addon-mode only.

5. GUI value editors (`core/gui/services/*`) likely render editors by Skript
   `ClassInfo` of a `CustomValue`. In standalone they need a Skript-free editor
   path keyed on the native type token (see Phase 2).

---

## Target design

### Mode detection

In `SkGame.onEnable()`:

```java
this.skriptPresent = getServer().getPluginManager().getPlugin("Skript") != null;
this.mode = skriptPresent ? Mode.ADDON : Mode.STANDALONE;
getLogUtil().info("SkGame booting in " + mode + " mode");
```

`plugin.yml`: `depend: [ Skript ]` → `softdepend: [ Skript ]` (keep SkBee, WorldGuard).

### Boot flow

```
onEnable:
  load config / messages / storage / db        (mode-independent)
  resolveModules()  = BUILTIN_MODULES + external jars (see extension-api.md)
  for each module: canEnable -> install resources -> onEnable
  if ADDON:
      build SkriptAddon
      register CORE_SKRIPT_CLASSES
      register each module.getSkriptClasses()
      install core scripts/ resources, accept .sk
  else STANDALONE:
      skip all Skript touch entirely
```

Every reference to `ch.njol.skript.*` in `core`/`SkGame` must be reachable **only**
when `mode == ADDON`. A safe pattern is a thin `addon` sub-package (new) that holds
all Skript-touching glue, instantiated reflectively/conditionally, so the classes
are never linked in standalone.

### Phase 2 — decouple `CustomValue` from Skript (the model fix)

Introduce a SkGame-native value type token and keep Skript as an adapter.

- New `api/game/model/type/ValueType` (enum or sealed interface):
  `TEXT, NUMBER, BOOLEAN, LOCATION, BLOCK, ITEM, REGION, PLAYER, …`
- `CustomValue` stores a `ValueType` (native) instead of / alongside `ClassInfo<?>`.
- Serialization of defaults uses Bukkit `ConfigurationSerializable` / primitives,
  not `ch.njol.skript.registrations.Classes`.
- Addon mode: a `SkriptTypeAdapter` maps `ValueType ↔ ClassInfo<?>` so existing
  Skript syntax (`type: a location`) keeps working.
- GUI value editors switch on `ValueType`, not `ClassInfo`.

**Recommended** over the cheaper "typeless in standalone" fallback, because native
extensions (Bridge Builders) need typed gamemap values (locations, regions) in
standalone mode to be configurable via the admin GUI.

### Phase 3 — standalone resource handling

- `resolveResourceTarget()` default: if Skript absent, a `scripts/` resource is a
  no-op (log at debug) — native extensions must not rely on `scripts/` for logic.
- Core `scripts/` install (`CORE_RESOURCE_PATHS`) runs only in addon mode.

---

## Phased rollout

- **Phase 1 — dual-mode skeleton + external loader** (branch `feat/standalone-mode`)
  - `plugin.yml` depend → softdepend
  - mode detection + `Mode` enum, logged
  - move all Skript-touching boot glue behind `mode == ADDON`
  - external module jar loader (`plugins/SkGame/modules/*.jar`) — see extension-api.md
  - **Guard, don't yet rewrite, `CustomValue`**: in standalone, if a module declares
    a value def with a Skript type, log a clear warning and treat as typeless.
    This lets Phase 1 compile and boot standalone even before Phase 2.
  - Acceptance: server with **no** Skript enables SkGame cleanly, loads an external
    module, `/skgame info` works; server **with** Skript behaves exactly as today.

- **Phase 2 — `CustomValue` / `ValueType` decouple** (own branch, reviewed)
- **Phase 3 — GUI editors on `ValueType`, resource polish, docs finalise**

---

## Risks

- Paper calls `onDisable()` before `isEnabled()=false` (already handled via explicit
  shutdown flag in `MiniGameManager`). Mirror that in any new teardown.
- External jar classloader must share SkGame's classloader as parent so `api` type
  casts/`instanceof` work. See extension-api.md.
- `CustomValue` touches Skript in `<clinit>`/field types — in standalone the class
  still **loads** (field is `ClassInfo<?>`, a Skript type). If Skript isn't on the
  classpath at all, classloading `CustomValue` throws `NoClassDefFoundError`.
  → Skript stays a `compileOnly`/provided dep; in standalone the Skript classes are
  simply absent at runtime, so **any** code path that links a Skript symbol must be
  unreachable. This is why Phase 2 (removing the `ClassInfo` field type) is required
  for a *fully* Skript-free standalone jar, and why Phase 1 only *guards call sites*.

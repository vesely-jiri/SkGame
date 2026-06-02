# SkGame

**SkGame** is a Skript addon for building multi-instance minigame frameworks on Paper servers. Game logic lives entirely in `.sk` scripts — the plugin provides the session lifecycle, GUI, player management, and region API so you don't have to.

## Requirements

| Dependency | Version | Notes |
|---|---|---|
| Paper | 1.21+ | Spigot not supported |
| Skript | 2.13+ | |
| SkBee | any | Optional — enables SkBee region support |
| WorldGuard | any | Optional — enables WorldGuard region support |

## Installation

1. Drop `SkGame.jar` into `plugins/`.
2. Restart the server.
3. Edit `plugins/SkGame/config.yml` as needed (modules, spectator gamemode, locale, etc.).
4. Write or install a `.sk` minigame script (see Quickstart below or bundled examples).

## Quickstart

Minimal minigame — register it, teleport players on start, declare a winner on stop:

```skript
on load:
    register minigame with id "example":
        set minigame name of event-minigame to "Example Game"
        set min players of event-minigame to 2
        set gamemap value "spawnpoints" of event-minigame to a custom value:
            set value name to "Spawn points"
            set value type to a location
            set plurality to plural

on "example" game start:
    set {_spawns::*} to gamemap values "spawnpoints" of event-session
    loop session players of event-session:
        teleport loop-player to {_spawns::1}

on "example" game stop:
    if event-string is "WIN":
        set winners of event-session to (temporary session value "winner" of event-session)
```

## Features

- **Multi-instance sessions** — unlimited concurrent games, each isolated
- **Teams** — built-in team assignment (auto, self-select, or both), per-team scores
- **Map voting** — players vote from a hotbar item during the preparation window
- **Spectator mode** — join mid-game, role switching, configurable gamemode
- **GUI system** — main menu, session lobby, minigame/map pickers, admin panel — all interceptable via Skript events
- **Stats & leaderboard** — win/play counts, top-player queries, per-player game history (SQLite)
- **Locale** — per-player locale (en/cs built-in), hot-reloadable YAML message files
- **Admin tools** — in-game map setup wand, gamemap value editor, session control panel
- **Region API** — CuboidRegion built-in; optional SkBee and WorldGuard adapters

## Links

- [GitHub Wiki — API Reference](https://github.com/vesely-jiri/SkGame/wiki)
- [Issues & bug reports](https://github.com/vesely-jiri/SkGame/issues)
- [Skript Discord](https://discord.gg/skript)

# SkGame

[![CI](https://github.com/vesely-jiri/SkGame/actions/workflows/ci.yml/badge.svg)](https://github.com/vesely-jiri/SkGame/actions/workflows/ci.yml)

**SkGame** is a Skript addon for building multi-instance minigame frameworks on Paper servers. Game logic lives entirely in `.sk` scripts — the plugin provides the session lifecycle, GUI, player management, region API, and locale system so you don't have to.

## Requirements

| Dependency | Version | Notes |
|---|---|---|
| Paper | 1.21+ | Spigot not supported |
| Skript | 2.13+ | |

## Installation

1. Drop `SkGame.jar` into `plugins/`.
2. Restart the server.
3. Edit `plugins/SkGame/config.yml` as needed (modules, spectator gamemode, locale, etc.).
4. Write or install a `.sk` minigame script (see Quickstart below or bundled examples).

## Quickstart

Minimal minigame — declare it, spread players to spawns, announce via locale:

```skript
locale "mygame":
    start:
        en: "&aGame started! Reach the goal to win."
        cs: "&aHra začala! Dosáhni cíle a vyhraj."

register minigame with id "mygame":
    name: "My Game"
    icon: diamond sword
    min players: 2
    time: 6000
    weather: "clear"
    cancel events: hunger, item-drop
    values:
        gamemap value "spawnpoints":
            name: "Spawn locations"
            type: a location
            plurality: plural

on "mygame" game start:
    spread shuffled (session players of event-session) across shuffled (gamemap values "spawnpoints" of event-session)
    send locale "mygame:start" to session players of event-session

on "mygame" game stop:
    teleport players of event-session to lobby
```

## Features

- **Multi-instance sessions** — unlimited concurrent games, each isolated
- **Declarative registration** — `register minigame with id "x":` top-level block with name, icon, min players, tags, time/weather, values, teams, cancel events
- **Teams** — built-in team assignment (auto, self-select, or both), per-team scores
- **Spread effect** — `spread %players% across %locations%` distributes players to spawns with automatic wrap-around
- **Zone API** — `%player% is in zone %string% of %session%` checks if a player is inside a named sub-region of the arena
- **Script locale system** — `locale "ns": key: en: "text"` block + `send locale / send title locale / send actionbar locale` effects; per-player language resolution
- **Session duration** — `session duration of %session%` returns elapsed time for timeout logic
- **Map voting** — players vote from a hotbar item during the preparation window
- **Spectator mode** — join mid-game, role switching, configurable gamemode
- **GUI system** — main menu, session lobby, minigame/map pickers, admin panel — all interceptable via Skript events
- **Stats & leaderboard** — win/play counts, top-player queries, per-player game history (SQLite)
- **System locale** — per-player locale (en/cs built-in), hot-reloadable YAML message files
- **Admin tools** — in-game map setup wand, gamemap value editor, session control panel
- **Region API** — CuboidRegion built-in

## Links

- [GitHub Wiki — API Reference](https://github.com/vesely-jiri/SkGame/wiki)
- [Issues & bug reports](https://github.com/vesely-jiri/SkGame/issues)
- [Skript Discord](https://discord.gg/skript)

# SkGame

**SkGame** is a Skript addon that provides support for managing minigames, maps, and player sessions on a Minecraft server.

## Features
- Manage **MiniGames** - register/unregister your minigame
- Manage **GameMaps** - register/unregister your game map that can support multiple minigames
- Manage player **Sessions** - Create session that can hold players, spectators, and more
- Automatic serialization and deserialization of MiniGames and GameMaps into YAML files

## Installation
1. Make sure you have the [Skript](https://github.com/SkriptLang/Skript) plugin installed.
2. [Optional] If you want to use SkBee Bounds or Worldguard regions, make sure to install those 
3. Place `SkGame.jar` into your server's `plugins` folder.
4. Start the server.

## Usage
- Register new game modes and maps via Skript sections/effects.
- Access sessions, players, and game data.
- Data is automatically saved and loaded from the `storage` folder.
- [Wiki](https://github.com/vesely-jiri/SkGame/wiki)

## Configuration
- All plugin data is stored in `plugins/SkGame/storage/` by default.
- `minigames.yml` – contains all registered game modes.
- `maps.yml` – contains all registered maps.

## Support
- For issues or questions, contact the developer or open an issue on the plugin repository.
# PointHome

A Minecraft Paper plugin that allows players to set and teleport to a home location with configurable delay, sound, and particle effects.

## Features
- **Set Home**: Players can set a home location using `/sethome`.
- **Teleport Home**: Use `/home` to teleport to the set home.
- **Teleport Delay**: Configurable delay before teleportation, cancellable if the player moves.
- **Customizable Effects**: Sound and particle effects for teleportation and delay phases.
- **Persistent Storage**: Home locations saved in `homes.yml`.
- **Multi-language Support**: Messages customizable via `config.yml`.

## Installation
1. Download the latest release from [Releases](https://github.com/Sitafe/Point-Home/releases).
2. Place the `.jar` file in your server's `plugins` folder.
3. Restart the server or use `/reload` to load the plugin.
4. Configure settings in `plugins/PointHome/config.yml` as needed.

## Commands
- `/sethome`: Sets the player's current location as their home.
- `/home`: Teleports the player to their home location.

## Configuration
The plugin uses two configuration files:
- **config.yml**: Customize messages, teleport delay, sounds, and particles.
- **homes.yml**: Stores player home locations (auto-generated).

### Example `config.yml`
```yaml
messages:
  set-home: "<green>Home successfully set!"
  home-teleport: "<blue>Teleporting home!"
  home-not-set: "<red>Error: Home not set."
  player-only: "<gray>This command is for players only."
  teleport-delay-start: "<yellow>Teleportation will start in <seconds> seconds. Don't move!"
  teleport-delay-cancelled: "<red>Teleportation cancelled: you moved."
teleport-delay:
  enabled: true
  seconds: 3
teleport-sound:
  enabled: true
  volume: 1.0
  pitch: 1.0
teleport-particles:
  enabled: true
  count: 50
delay-particles:
  enabled: true
  count: 5
```

## Requirements
- **Server**: Paper 1.21 or compatible versions.
- **Dependencies**: None (standalone plugin).

## Building
1. Clone the repository: `git clone https://github.com/your-repo/PointHome.git`
2. Navigate to the project directory: `cd PointHome`
3. Build with Maven: `mvn clean package`
4. Find the compiled `.jar` in the `target` folder.

## License
MIT License. See [LICENSE](LICENSE) for details.

## Contributing
Contributions are welcome! Please submit a pull request or open an issue for bugs or feature requests.

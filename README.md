# RedClient

A comprehensive Hypixel Skyblock automation and utility mod for Minecraft 1.21.11, built with Fabric. RedClient provides powerful tools for fishing, combat, hunting, and macro scheduling with an intuitive YACL v3 configuration interface.

![RedClient Logo](RedClientLogo.jpg)

## Features

### 🎣 Fishing & Auto-Fishing
- **Auto Fishing**: Automated fishing with configurable recast and reeling delays
- **Sea Creature Killer**: Automatically detects and eliminates sea creatures
- **Spike Helper**: Highlights and assists with Spike combat encounters
- **Bezal Farmer**: Automated Bezal handling and farming
- **Cinderbat Highlighting**: Smart detection and through-wall outlining of Cinderbats with configurable range
  - Detects Cinderbats by health stats (4.8M+ HP or 1000+ HP with max health)
  - Through-wall outline rendering for easy tracking
  - Configurable detection range (default 220 blocks, max 500)
  - Debug logging for matched bats and scan counts
  - One-time coordinate message on first detection
  - Throttled scanning for performance (500ms idle, 2000ms tracking)
  - Toggle with keybind (default J key)

### ⚔️ Combat Features
- **Flare Combat Macro**: Automated Flare combat sequences with customizable click patterns
- **Multiple Combat Modes**: 
  - Melee Mode: Direct engagement with mobs
  - RCM Mode: Advanced combat mechanics

### 🎯 Hunting & Detection
- **Mob Highlighting**: Visual highlighting for various mob types
- **Entity Outlining**: Through-wall outlines for targets
- **Combat Mode Management**: Easy switching between combat strategies

### ⏰ Macro Scheduler
- **Break Scheduling**: Automated break reminders and enforced AFK periods
- **Configurable Timers**: Set custom durations for work/break cycles
- **Smart Scheduling**: Manage multiple macro sessions

### 🔧 Utilities
- **XYZ Macro**: Position-based macro execution
- **Mouse Simulator**: Realistic mouse movement and clicking
- **Weapon Detector**: Automatic weapon detection for combat
- **Rotation Manager**: Smooth player rotation controls

## Installation

### Requirements
- Minecraft 1.21.11
- Fabric Loader 0.18.4+
- Fabric API 0.141.3+1.21.11

### Setup
1. Download the latest JAR from [Releases](https://github.com/ransudev/RedClient/releases)
2. Place it in your `mods` folder
3. Launch Minecraft with Fabric
4. Join Hypixel Skyblock

## Configuration

Open the config GUI with:
- **In-game command**: `/red gui` or `/redgui`
- **ModMenu button**: Click the RedClient option in ModMenu settings
- **Keybind**: Press **R** (configurable)

The configuration interface is built with **YACL v3** and includes:

### Fishing Settings
- Auto Fishing toggle
- Recast delay (ticks)
- Reeling delay (ticks)
- Sea Creature Killer toggle
- Combat mode selection
- Spike Helper options
- Bezal Farmer configuration
- Cinderbat highlighting settings
  - Enable/disable toggle
  - Detection range (220-500 blocks)
  - Debug logging toggle

### Hunting Settings
- Mob Highlighting options
- Entity outline configurations
- Combat preferences

### Spike Helper
- Enable/disable
- Detection range
- Outline color customization

### Scheduler
- Break duration
- Work duration
- Auto-start toggle

## Commands

### `/fish` - Fishing Control
- `/fish auto <true/false>` - Toggle auto fishing
- `/fish seacreaturekiller <true/false>` - Toggle sea creature killer
- `/fish spike <true/false>` - Toggle spike helper
- `/fish bezal <true/false>` - Toggle Bezal farmer
- `/fish cinderbat <true/false>` - Toggle Cinderbat highlighting
- `/fish cinderdebug <true/false>` - Toggle debug logging for Cinderbat scanning

### `/flare` - Flare Combat Control
- `/flare macro <true/false>` - Toggle Flare macro
- `/flare clicks <number>` - Set click count

### `/red` - General Commands
- `/red gui` - Open configuration GUI
- `/red help` - Display command help

### `/redgui` - Quick GUI Access
- Opens the configuration screen directly

## Keybindings

| Key | Function | Configurable |
|-----|----------|--------------|
| **R** | Open Configuration GUI | ✓ |
| **J** | Toggle Cinderbat Highlighting | ✓ |
| *Custom* | Auto Fishing Toggle | ✓ |
| *Custom* | Flare Macro Toggle | ✓ |

All keybinds can be customized in Minecraft's Controls menu.

## Technical Details

### Architecture
- **Mixins**: Native entity rendering and input handling via Mixins
- **Event Listeners**: Tick events for features and command registration
- **GSON Configuration**: Persistent JSON-based config files
- **YACL v3 UI**: Modern configuration interface with sliders, toggles, and descriptions

### Config Files
Configuration files are stored in `.minecraft/config/`:
- `autofish.json` - Fishing automation settings
- `flarecombat.json` - Flare combat configuration
- `mobhighlight.json` - Mob highlighting settings
- `cinderbat.json` - Cinderbat detection configuration
- `macroscheduler.json` - Scheduler settings

### Dependencies
- **Fabric API**: Core modding framework
- **OWO-LIB**: Utility library for UI and events
- **ModMenu**: In-game mod configuration menu
- **YACL v3**: Modern configuration GUI framework

## Development

### Build
```bash
./gradlew build
```

### Run Development Environment
```bash
./gradlew runClient
```

### Reload in Dev
Changes to configs and features are hot-reloadable during development.

## Troubleshooting

### Features Not Working
1. Check that the feature is **enabled** in the config GUI
2. Verify the **keybind** isn't conflicting with other mods
3. Look at console output for error messages
4. Check that you're in **Hypixel Skyblock**, not other game modes

### Cinderbat Highlighting Issues
- Ensure detection range is appropriate for your system (higher = more CPU usage)
- Check debug logging to see if bats are being detected
- Verify the health stat detection thresholds match the current Skyblock version

### Performance Issues
- Reduce Cinderbat detection range if experiencing lag
- Disable unnecessary highlighting features
- Check Minecraft's debug screen (F3) for TPS/FPS

### Config Not Saving
- Ensure `.minecraft/config/` folder exists and is writable
- Check console for save error messages
- Restart the game to reload default config

## Contributing

Found a bug or want to suggest a feature? Feel free to open an [issue](https://github.com/ransudev/RedClient/issues) or submit a pull request.

## License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.

## Credits

Built with:
- [Fabric](https://fabricmc.net/) - Lightweight modding framework
- [YACL](https://github.com/isXander/YACL) - Modern config GUI
- [OWO-LIB](https://github.com/wisp-forest/owo-lib) - Utilities and events

## Support

For issues, questions, or feature requests:
- **GitHub Issues**: [Report a bug](https://github.com/ransudev/RedClient/issues)
- **GitHub Discussions**: [Ask a question](https://github.com/ransudev/RedClient/discussions)

---

**Last Updated**: Minecraft 1.21.11 | Fabric 0.18.4+

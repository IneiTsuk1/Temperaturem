# Temperaturem

A comprehensive Minecraft temperature survival mod that adds realistic environmental temperature mechanics, requiring players to manage their body temperature to survive in extreme climates.

![Minecraft Version](https://img.shields.io/badge/Minecraft-1.20.1-brightgreen)
![Mod Loader](https://img.shields.io/badge/Loader-Fabric-blue)
![License](https://img.shields.io/badge/License-ILRL-yellow)

## Features

### Dynamic Temperature System
- **Biome-based temperatures** - Each biome has its own base temperature
- **Block heat sources** - Lava, fire, campfires, and more affect nearby temperature
- **Time-based variations** - Temperatures fluctuate between day and night
- **Altitude effects** - Higher elevations are colder
- **Weather impact** - Rain and storms reduce temperature
- **Shelter mechanics** - Being under a roof provides warmth

### Armor Underlay System
Craft and apply temperature-regulating underlays to your armor for protection against extreme environments!

#### Cold Protection Underlays
- **Wool Lining** - Basic cold protection (+8°C, 500 durability)
- **Fur Lining** - Good cold protection (+15°C, 800 durability)
- **Thermal Padding** - Excellent cold protection (+25°C, 1200 durability)
- **Insulated Fabric** - Moderate cold protection (+12°C, 600 durability)

#### Heat Protection Underlays
- **Leather Lining** - Basic heat protection (-8°C, 500 durability)
- **Cooling Mesh** - Good heat protection (-15°C, 800 durability)
- **Climate Weave** - Excellent heat protection (-25°C, 1200 durability)
- **Reflective Layer** - Moderate heat protection (-12°C, 600 durability)

> High-tier underlays have an enchantment glint effect

### Temperature Zones
- Define temperature-controlled areas using 3D zones
- **Absolute zones** fully override environmental temperature (e.g. heated buildings)
- **Additive zones** modify natural temperature (e.g. heaters, air conditioning)
- Smooth transitions near zone borders
- Priority-based handling for overlapping zones
- Zones can be enabled or disabled at runtime

### Temperature Zone Commands
- `/tempzone create <name> <pos1> <pos2> <temp> <ABSOLUTE|ADDITIVE>`
- `/tempzone list` – List all zones in the world
- `/tempzone info <uuid>` – View detailed zone info
- `/tempzone find <name>` – Search zones by name
- `/tempzone settemp <uuid> <value>` – Change zone temperature
- `/tempzone setpriority <uuid> <value>` – Set zone priority
- `/tempzone toggle <uuid>` – Enable or disable a zone
- `/tempzone here` – Show zones affecting your current position

### Temperature Effects

#### Cold Temperature Ranges
- **0°C to -10°C (Cold)** - Slowness I
- **-10°C to -20°C (Very Cold)** - Slowness II, Mining Fatigue I
- **Below -20°C (Extreme Cold)** - Slowness III, Mining Fatigue II, Weakness II, damage over time

#### Hot Temperature Ranges
- **40°C to 55°C (Hot)** - Hunger
- **55°C to 65°C (Very Hot)** - Weakness I, Hunger, Slowness I
- **Above 65°C (Extreme Hot)** - Weakness II, Nausea, Slowness II, damage over time

#### Comfortable Range
- **12°C to 25°C** - Slight Regeneration bonus!

### Gameplay Mechanics

**Temperature Display** - Your current temperature is displayed on-screen (configurable)

**Action Bar Warnings** - Receive alerts when entering dangerous temperature zones

**Gradual Changes** - Temperature changes smoothly based on your environment

**Underlay Durability** - Underlays wear down faster in extreme conditions they protect against

**Strategic Choices** - Decide whether to protect against cold or heat based on your journey

## Installation

1. Install [Fabric Loader](https://fabricmc.net/use/)
2. Download [Fabric API](https://modrinth.com/mod/fabric-api)
3. Download Temperaturem
4. Place both `.jar` files in your `.minecraft/mods` folder
5. Launch Minecraft with the Fabric profile

## How to Use

### Applying Underlays
1. Craft an underlay item (recipes available in-game)
2. Open your inventory
3. **Left-click** the underlay item on any armor piece to apply it
4. The underlay is now attached and providing protection!

### Removing Underlays
1. **Shift + Right-click** on armor with an attached underlay
2. The underlay will be removed and returned to your inventory
3. Durability is preserved when removed

## Configuration

Configuration files are located in `config/temperaturem/`

### `effects.json`
```json
{
  "enableDamage": true,
  "enableStatusEffects": true,
  "enableWarnings": true,
  "damageInterval": 40,
  "coldDamage": 1.0,
  "hotDamage": 1.0
}
```

### `blocks/temperature_blocks.json`
Define custom block temperatures:
```json
{
  "minecraft:lava": "100",
  "minecraft:fire": "80",
  "minecraft:campfire": "60|70",
  "minecraft:ice": "-10"
}
```

### `biomes/biome_temperatures.json`
Override biome temperatures:
```json
{
  "minecraft:desert": "45",
  "minecraft:snowy_taiga": "-15",
  "minecraft:badlands": "40"
}
```

## For Mod Developers

Temperaturem provides a comprehensive API for integrating temperature mechanics into your mod. See [API_README.md](API_README.md) for detailed documentation.

**Quick Example:**
```java
TemperatureAPI api = TemperatureAPI.getInstance();

// Get player temperature
int temp = api.getPlayerTemperature(player);

// Register custom blocks
api.registerBlockTemperature(
    new Identifier("mymod", "hot_rock"), 
    80
);

// Modify player temperature
api.modifyPlayerTemperature(player, -10.0);
```

**Temperature Zones:**
```
// Create a temperature-controlled zone
TemperatureAPI api = TemperatureAPI.getInstance();

TemperatureZone zone = api.createZone(
    world,
    "Heated Building",
    corner1,
    corner2,
    22.0,
    TemperatureZone.ZoneType.ABSOLUTE
);

// Query zones at a position
List<TemperatureZone> zones = api.getZonesAt(world, playerPos);

// Get effective zone temperature
Double zoneTemp = api.getZoneTemperatureAt(world, playerPos);

```

## Compatibility

- **Minecraft**: 1.20.1
- **Fabric Loader**: Latest
- **Fabric API**: Required

### Known Compatible Mods
- Biome mods (just add them to the biome config file)
- Armor mods (underlays work on any armor)

## Performance

Temperaturem is designed with performance in mind:
- Efficient temperature calculations with caching
- Concurrent-safe data structures
- Configurable update intervals
- Minimal network traffic

## Credits

**Developer:** IneiTsuki

## License

This project is licensed under the ILRL License - see the LICENSE file for details.

## Support

- **Issues:** [GitHub Issues](https://github.com/IneiTsuk1/Temperaturem/issues)

## Changelog

### Version 1.0.4
- Added TemperatureAPI temperature zones
- Support for absolute and additive temperature-controlled areas
- Priority-based zone resolution and smooth transitions
- In-game commands for zone management
- Expanded public API for mod integration

---

# Temperature Mod for Minecraft

A realistic temperature system for Minecraft that dynamically affects players based on biome, blocks, weather, time of day, and environment. Designed for modpacks and server gameplay, with API support for other mods to interact with player temperature.

---

## Features

- **Dynamic Player Temperature**
  - Changes based on biome, altitude, time of day, weather, and nearby blocks.
  - Gradual temperature adjustments with smooth transitions.

- **Biome Overrides**
  - Custom biome temperatures configurable via JSON.
  - Modded biome support included, just add in the JSON in the correct format (example given in config files)

- **Block Influence**
  - Blocks can have a temperature so that blocks such as ice will cool down a player while normal blocks like dirt/sand etc dont affect your temperature.
  - Multiple blocks balance each other; diminishing returns prevent unrealistic extremes.
  - For modded biomes that have temperature support from this mod, the blocks from extreme heat/cold biomes can help reduce/increase heat in certain biomes

- **Environmental Modifiers**
  - Weather effects: rain, thunderstorms, snow.
  - Shelter under roofs reduces exposure to weather penalties.
  - Altitude and underground effects on temperature.

- **API for Other Mods**
  - Access via `TemperatureAPI.getInstance()`.
  - Methods to get/set player temperature, and query block/biome temperatures.
  - Planned future support for registering blocks/biomes at runtime.

---

## Installation

1. Place the mod JAR in your `mods/` folder.
2. Ensure Fabric API and Fabric Loader are installed.
3. Start Minecraft; config files will generate automatically under:

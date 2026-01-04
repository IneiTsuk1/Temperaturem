# Temperaturem API Documentation

A comprehensive API for integrating temperature mechanics into your Minecraft mod.

## Table of Contents
- [Getting Started](#getting-started)
- [Player Temperature Management](#player-temperature-management)
- [Runtime Registration](#runtime-registration)
- [Temperature Zones](#temperature-zones)
- [Query Methods](#query-methods)
- [Effect Configuration](#effect-configuration)
- [Advanced Usage](#advanced-usage)
- [Best Practices](#best-practices)
- [Error Handling](#error-handling)
- [API Reference](#api-reference-summary)
- [Support](#support)
- [Version Compatibility](#version-compatibility)

## Getting Started

### Adding the Dependency

Add Temperaturem to your `build.gradle`:

```gradle
repositories {
    maven { url "https://jitpack.io" }
}

dependencies {
    modImplementation "com.github.IneiTsuk1:Temperaturem:VERSION"
}
```

### Accessing the API

The API is accessed through a singleton instance:

```java
import net.IneiTsuki.temperaturem.api.TemperatureAPI;

TemperatureAPI api = TemperatureAPI.getInstance();
```
The API is server-side only and safe to cache.

## Player Temperature Management

### Getting Player Temperature

```java
// Get rounded temperature
int temperature = api.getPlayerTemperature(player);

// Get exact temperature with decimals
double exactTemp = api.getPlayerTemperatureExact(player);
```

**Temperature Ranges:**
- `-50°C to 150°C` - Valid range (automatically clamped)
- `12°C to 25°C` - Comfortable range
- `< -20°C` - Extreme cold (lethal)
- `> 65°C` - Extreme heat (lethal)

### Modifying Player Temperature

```java
// Cool the player by 10 degrees
api.modifyPlayerTemperature(player, -10.0);

// Heat the player by 15 degrees
api.modifyPlayerTemperature(player, 15.0);

// Set exact temperature (bypasses gradual changes)
api.setPlayerTemperature(player, 0.0);
```

### Example: Potion Effect

Create a cooling potion effect:

```java
public class CoolingPotionEffect extends StatusEffect {
    @Override
    public void applyUpdateEffect(LivingEntity entity, int amplifier) {
        if (entity instanceof ServerPlayerEntity player) {
            TemperatureAPI api = TemperatureAPI.getInstance();
            
            // Cool by 0.5°C per tick at level 1
            double cooling = -0.5 * (amplifier + 1);
            api.modifyPlayerTemperature(player, cooling);
        }
    }
}
```

## Runtime Registration

Register custom temperature sources at runtime (persists across resource reloads).

### Block Temperature Registration

```java
// Register a hot block
api.registerBlockTemperature(
    new Identifier("mymod", "volcanic_stone"),
    85  // 85°C
);

// Register a cold block
api.registerBlockTemperature(
    new Identifier("mymod", "frozen_crystal"),
    -15  // -15°C
);
```

**Valid Temperature Range:** `-273°C` to `1000°C`

**Throws:**
- `IllegalArgumentException` if block doesn't exist
- `IllegalArgumentException` if temperature is out of range

### Biome Temperature Registration

```java
// Register a hot biome
api.registerBiomeTemperature(
    new Identifier("mymod", "volcanic_wasteland"),
    95  // 95°C base temperature
);

// Register a cold biome
api.registerBiomeTemperature(
    new Identifier("mymod", "frozen_tundra"),
    -25  // -25°C base temperature
);
```

### Unregistering

```java
// Remove block temperature
boolean removed = api.unregisterBlockTemperature(
    new Identifier("mymod", "volcanic_stone")
);

// Remove biome temperature
boolean removed = api.unregisterBiomeTemperature(
    new Identifier("mymod", "volcanic_wasteland")
);
```
## Temperature Zones

Temperature Zones allow localized 3D areas with controlled temperature behavior, either additive or absolute.

### Zone Types
#### Absolute - completley overrides environmental temperature.
#### Additive - Adds to/subtracts from the natural temperature.

### Creating a Zone
```
TemperatureZone zone = api.createZone(
    world,
    "Heated Building",
    new BlockPos(100, 64, 100),
    new BlockPos(120, 80, 120),
    22.0,
    TemperatureZone.ZoneType.ABSOLUTE
);
```

### Additive Zone Example
```
TemperatureZone coolingZone = api.createZone(
    world,
    "Air Conditioning",
    new BlockPos(50, 64, 50),
    new BlockPos(70, 75, 70),
    -15.0,
    TemperatureZone.ZoneType.ADDITIVE
);

coolingZone.setTransitionRange(5.0); // Smooth blending over 5 blocks
coolingZone.setPriority(10);          // Higher priority than default
```

### Updating Zone Properties
```
api.setZoneTemperature(world, zone.getId(), 20.0);
api.setZoneEnabled(world, zone.getId(), false);
```

### Querying Zones
```
List<TemperatureZone> zones = api.getZonesAt(world, player.getBlockPos());
Double zoneTemp = api.getZoneTemperatureAt(world, player.getBlockPos());
List<TemperatureZone> foundZones = api.findZonesByName(world, "building");
int totalZones = api.getZoneCount(world);
long totalVolume = api.getTotalZoneVolume(world);
```

### Example: Dynamic Temperature System

```java
public class VolcanicEruption {
    private final TemperatureAPI api = TemperatureAPI.getInstance();
    
    public void startEruption(BlockPos center, World world) {
        // Heat up lava blocks during eruption
        api.registerBlockTemperature(
            new Identifier("minecraft", "lava"),
            150  // Much hotter during eruption
        );
    }
    
    public void endEruption() {
        // Return to normal temperature
        api.registerBlockTemperature(
            new Identifier("minecraft", "lava"),
            100  // Normal lava temperature
        );
    }
}
```

## Query Methods

### Checking Temperature Sources

```java
// Check if a block has temperature
Identifier blockId = new Identifier("minecraft", "campfire");
if (api.hasBlockTemperature(blockId)) {
    int temp = api.getBlockTemperature(blockId);
    System.out.println("Campfire temperature: " + temp + "°C");
}

// Check biome temperature
Identifier biomeId = new Identifier("minecraft", "desert");
if (api.hasBiomeTemperature(biomeId)) {
    Integer temp = api.getBiomeTemperature(biomeId);
    System.out.println("Desert temperature: " + temp + "°C");
}
```

### Example: Temperature Scanner Tool

```java
public class TemperatureScannerItem extends Item {
    private final TemperatureAPI api = TemperatureAPI.getInstance();
    
    @Override
    public TypedActionResult use(World world, PlayerEntity user, Hand hand) {
        if (user instanceof ServerPlayerEntity player) {
            BlockPos pos = player.getBlockPos();
            BlockState state = world.getBlockState(pos);
            Identifier blockId = Registries.BLOCK.getId(state.getBlock());
            
            // Scan current block
            if (api.hasBlockTemperature(blockId)) {
                int temp = api.getBlockTemperature(blockId);
                player.sendMessage(
                    Text.literal("Block temperature: " + temp + "°C"),
                    false
                );
            }
            
            // Scan current biome
            Identifier biomeId = world.getBiome(pos).getKey()
                .map(key -> key.getValue())
                .orElse(null);
            
            if (biomeId != null && api.hasBiomeTemperature(biomeId)) {
                int temp = api.getBiomeTemperature(biomeId);
                player.sendMessage(
                    Text.literal("Biome temperature: " + temp + "°C"),
                    false
                );
            }
            
            // Show player temperature
            int playerTemp = api.getPlayerTemperature(player);
            player.sendMessage(
                Text.literal("Your temperature: " + playerTemp + "°C"),
                false
            );
        }
        
        return TypedActionResult.success(user.getStackInHand(hand));
    }
}
```

## Effect Configuration

### Temperature Damage

```java
// Enable/disable damage
api.setEnableTemperatureDamage(true);

// Configure damage amounts
api.setColdDamage(2.0f);  // 2 damage (1 heart)
api.setHotDamage(1.5f);   // 1.5 damage (0.75 hearts)

// Set damage interval (ticks)
api.setDamageInterval(40);  // Every 2 seconds

// Query current settings
boolean damageEnabled = api.isTemperatureDamageEnabled();
float coldDamage = api.getColdDamage();
int interval = api.getDamageInterval();
```

### Status Effects

```java
// Enable/disable status effects
api.setEnableStatusEffects(true);

// Query setting
boolean effectsEnabled = api.isStatusEffectsEnabled();
```

**Status Effects Applied:**
- **Cold:** Slowness, Mining Fatigue, Weakness
- **Hot:** Weakness, Nausea, Hunger, Slowness
- **Comfortable:** Regeneration

### Warning Messages

```java
// Enable/disable action bar warnings
api.setEnableWarnings(true);

// Query setting
boolean warningsEnabled = api.isWarningsEnabled();
```

### Example: Difficulty-Based Configuration

```java
public class TemperatureDifficultyManager {
    private final TemperatureAPI api = TemperatureAPI.getInstance();
    
    public void configureDifficulty(Difficulty difficulty) {
        switch (difficulty) {
            case PEACEFUL -> {
                api.setEnableTemperatureDamage(false);
                api.setEnableStatusEffects(false);
                api.setEnableWarnings(true);
            }
            case EASY -> {
                api.setEnableTemperatureDamage(true);
                api.setColdDamage(0.5f);
                api.setHotDamage(0.5f);
                api.setDamageInterval(60);  // Every 3 seconds
            }
            case NORMAL -> {
                api.setEnableTemperatureDamage(true);
                api.setColdDamage(1.0f);
                api.setHotDamage(1.0f);
                api.setDamageInterval(40);  // Every 2 seconds
            }
            case HARD -> {
                api.setEnableTemperatureDamage(true);
                api.setColdDamage(2.0f);
                api.setHotDamage(2.0f);
                api.setDamageInterval(20);  // Every 1 second
            }
        }
    }
}
```

## Advanced Usage

### Custom Armor Protection System

```java
public class CustomArmorProtection {
    private final TemperatureAPI api = TemperatureAPI.getInstance();
    
    @Mixin(PlayerEntity.class)
    public class PlayerArmorMixin {
        @Inject(method = "tick", at = @At("TAIL"))
        private void onTick(CallbackInfo ci) {
            PlayerEntity player = (PlayerEntity) (Object) this;
            if (!(player instanceof ServerPlayerEntity serverPlayer)) return;
            
            // Check for custom armor set bonus
            if (hasFullFireArmorSet(player)) {
                // Reduce temperature if too hot
                double temp = api.getPlayerTemperatureExact(serverPlayer);
                if (temp > 25.0) {
                    api.modifyPlayerTemperature(serverPlayer, -0.1);
                }
            }
        }
    }
}
```

### Temperature-Based World Events

```java
public class TemperatureEventHandler {
    private final TemperatureAPI api = TemperatureAPI.getInstance();
    
    public void handleExtremeCold(ServerPlayerEntity player, BlockPos pos) {
        double temp = api.getPlayerTemperatureExact(player);
        
        if (temp < -30.0) {
            // Spawn ice crystals
            World world = player.getWorld();
            world.setBlockState(pos.up(), Blocks.ICE.getDefaultState());
            
            // Further cool the player
            api.modifyPlayerTemperature(player, -5.0);
            
            // Notify player
            player.sendMessage(
                Text.literal("Your body heat is freezing the air around you!")
                    .formatted(Formatting.AQUA),
                false
            );
        }
    }
    
    public void handleExtremeHeat(ServerPlayerEntity player) {
        double temp = api.getPlayerTemperatureExact(player);
        
        if (temp > 80.0) {
            // Set blocks on fire
            BlockPos pos = player.getBlockPos();
            World world = player.getWorld();
            
            for (BlockPos nearbyPos : BlockPos.iterate(pos.add(-1, -1, -1), pos.add(1, 0, 1))) {
                if (world.isAir(nearbyPos)) {
                    world.setBlockState(nearbyPos, Blocks.FIRE.getDefaultState());
                }
            }
        }
    }
}
```

### Integration with Custom Dimensions

```java
public class NetherTemperatureModifier implements ModInitializer {
    @Override
    public void onInitialize() {
        ServerTickEvents.END_WORLD_TICK.register(world -> {
            if (world.getRegistryKey() == World.NETHER) {
                TemperatureAPI api = TemperatureAPI.getInstance();
                
                for (ServerPlayerEntity player : world.getPlayers()) {
                    // Gradual heating in the Nether
                    double currentTemp = api.getPlayerTemperatureExact(player);
                    if (currentTemp < 80.0) {
                        api.modifyPlayerTemperature(player, 0.2);
                    }
                }
            }
        });
    }
}
```

### Custom Temperature HUD

```java
public class TemperatureHudOverlay {
    private final TemperatureAPI api = TemperatureAPI.getInstance();
    
    public void render(DrawContext context, MinecraftClient client) {
        if (client.player == null) return;
        
        // This would need to be called from server and synced to client
        // or query local player temperature
        
        // Example rendering code
        int temperature = getLocalTemperature();  // Implement client-side caching
        
        String tempText = temperature + "°C";
        int color = getTemperatureColor(temperature);
        
        context.drawTextWithShadow(
            client.textRenderer,
            tempText,
            10, 10,
            color
        );
    }
    
    private int getTemperatureColor(int temp) {
        if (temp < -20) return 0x00FFFF;  // Cyan - extreme cold
        if (temp < 0) return 0x5555FF;     // Blue - cold
        if (temp > 65) return 0xFF0000;    // Red - extreme heat
        if (temp > 40) return 0xFF5555;    // Light red - hot
        return 0xFFFFFF;                   // White - comfortable
    }
}
```

## Best Practices

### 1. **Always Check for Null**
```java
if (player != null) {
    api.modifyPlayerTemperature(player, delta);
}
```

### 2. **Use Gradual Changes**
```java
// Good: Gradual change
ServerTickEvents.END_SERVER_TICK.register(server -> {
    api.modifyPlayerTemperature(player, 0.1);  // Small increments
});

// Bad: Instant extreme change
api.setPlayerTemperature(player, 100.0);  // Too sudden
```

### 3. **Validate Temperature Ranges**
```java
public void setCustomTemperature(ServerPlayerEntity player, double temp) {
    // Clamp to reasonable values
    temp = Math.max(-50, Math.min(150, temp));
    api.setPlayerTemperature(player, temp);
}
```

### 4. **Handle Exceptions**
```java
try {
    api.registerBlockTemperature(blockId, temperature);
} catch (IllegalArgumentException e) {
    LOGGER.error("Failed to register block temperature: " + e.getMessage());
}
```

### 5. **Cache API Instance**
```java
public class MyMod {
    private static final TemperatureAPI TEMP_API = TemperatureAPI.getInstance();
    
    // Use TEMP_API throughout your mod
}
```

### 6. **Server-Side Only**
The API is **server-side only**. All temperature calculations happen on the server and are synced to clients automatically.

```java
// Good
if (!world.isClient) {
    api.modifyPlayerTemperature(player, delta);
}

// Bad - will crash on client
api.modifyPlayerTemperature(player, delta);  // No client check
```

## Error Handling

### Common Exceptions

| Exception | Cause | Solution |
|-----------|-------|----------|
| `IllegalArgumentException` | Null identifier | Check identifiers before passing |
| `IllegalArgumentException` | Block doesn't exist | Verify block is registered |
| `IllegalArgumentException` | Temperature out of range | Clamp to -273°C to 1000°C |
| `IllegalArgumentException` | Negative damage value | Use positive values only |
| `IllegalArgumentException` | Invalid tick interval | Use values ≥ 1 |

### Example Error Handling

```java
public boolean registerSafely(Identifier blockId, int temperature) {
    try {
        api.registerBlockTemperature(blockId, temperature);
        return true;
    } catch (IllegalArgumentException e) {
        LOGGER.warn("Failed to register temperature for {}: {}", 
                   blockId, e.getMessage());
        return false;
    }
}
```

## API Reference Summary

### Player Temperature
- `int getPlayerTemperature(ServerPlayerEntity)`
- `double getPlayerTemperatureExact(ServerPlayerEntity)`
- `void modifyPlayerTemperature(ServerPlayerEntity, double)`
- `void setPlayerTemperature(ServerPlayerEntity, double)`

### Runtime Registration
- `void registerBlockTemperature(Identifier, int)`
- `void registerBiomeTemperature(Identifier, int)`
- `boolean unregisterBlockTemperature(Identifier)`
- `boolean unregisterBiomeTemperature(Identifier)`

### Temperature Zones
- `TemperatureZone createZone(World, String, BlockPos, BlockPos, double, ZoneType)`
- `void setZoneTemperature(World, UUID, double)`
- `void setZoneEnabled(World, UUID, boolean)`
- `List<TemperatureZone> getZonesAt(World, BlockPos)`
- `Double getZoneTemperatureAt(World, BlockPos)`
- `List<TemperatureZone> findZonesByName(World, String)`
- `int getZoneCount(World)`
- `long getTotalZoneVolume(World)`

### Query Methods
- `int getBlockTemperature(Identifier)`
- `Integer getBiomeTemperature(Identifier)`
- `boolean hasBlockTemperature(Identifier)`
- `boolean hasBiomeTemperature(Identifier)`

### Effect Configuration
- `void setEnableTemperatureDamage(boolean)`
- `void setEnableStatusEffects(boolean)`
- `void setEnableWarnings(boolean)`
- `void setDamageInterval(int)`
- `void setColdDamage(float)`
- `void setHotDamage(float)`
- `boolean isTemperatureDamageEnabled()`
- `boolean isStatusEffectsEnabled()`
- `boolean isWarningsEnabled()`
- `int getDamageInterval()`
- `float getColdDamage()`
- `float getHotDamage()`

## Support

For API support and questions:
- **GitHub Issues:** [Report bugs or request features](https://github.com/IneiTsuk1/Temperaturem/issues)
- **Documentation:** Check the source code for detailed JavaDoc comments
- **Examples:** See the `examples/` directory in the repository

## Version Compatibility

| Temperaturem Version | API Version | Minecraft Version |
|---------------------|-------------|-------------------|
| 1.0.4 | 1.0 | 1.20.1 |

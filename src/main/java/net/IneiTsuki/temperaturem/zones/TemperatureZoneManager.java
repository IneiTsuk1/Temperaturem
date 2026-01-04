package net.IneiTsuki.temperaturem.zones;

import net.IneiTsuki.temperaturem.Temperaturem;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.PersistentState;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class TemperatureZoneManager extends PersistentState {

    private static final String DATA_NAME = "temperaturem_zones";
    private static final Map<ServerWorld, TemperatureZoneManager> INSTANCES = new ConcurrentHashMap<>();

    private final Map<UUID, TemperatureZone> zones = new ConcurrentHashMap<>();
    private final ServerWorld world;

    // Cache for zone lookups by position
    private final Map<BlockPos, List<TemperatureZone>> positionCache = new ConcurrentHashMap<>();
    private int cacheClearCounter = 0;
    private static final int CACHE_CLEAR_INTERVAL = 200; // Clear cache every 10 seconds

    public TemperatureZoneManager(ServerWorld world) {
        this.world = world;
    }

    // ===== Static Access =====

    public static TemperatureZoneManager get(ServerWorld world) {
        return INSTANCES.computeIfAbsent(world, w -> {
            TemperatureZoneManager manager = w.getPersistentStateManager()
                    .getOrCreate(
                            nbt -> fromNbt(w, nbt),
                            () -> new TemperatureZoneManager(w),
                            DATA_NAME
                    );
            return manager;
        });
    }

    public static void clearInstances() {
        INSTANCES.clear();
    }

    // ===== Zone Management =====

    public TemperatureZone createZone(String name, Box bounds, double temperature, TemperatureZone.ZoneType type) {
        UUID id = UUID.randomUUID();
        TemperatureZone zone = new TemperatureZone(id, name, bounds, temperature, type);
        zones.put(id, zone);
        clearPositionCache();
        markDirty();
        Temperaturem.LOGGER.info("Created temperature zone: {}", zone);
        return zone;
    }

    public void addZone(TemperatureZone zone) {
        zones.put(zone.getId(), zone);
        clearPositionCache();
        markDirty();
    }

    public boolean removeZone(UUID id) {
        TemperatureZone removed = zones.remove(id);
        if (removed != null) {
            clearPositionCache();
            markDirty();
            Temperaturem.LOGGER.info("Removed temperature zone: {}", removed.getName());
            return true;
        }
        return false;
    }

    public TemperatureZone getZone(UUID id) {
        return zones.get(id);
    }

    public Collection<TemperatureZone> getAllZones() {
        return Collections.unmodifiableCollection(zones.values());
    }

    public List<TemperatureZone> getZonesAt(BlockPos pos) {
        // Check cache first
        if (positionCache.containsKey(pos)) {
            return positionCache.get(pos);
        }

        List<TemperatureZone> result = zones.values().stream()
                .filter(zone -> zone.isEnabled() && zone.contains(pos))
                .sorted((z1, z2) -> Integer.compare(z2.getPriority(), z1.getPriority()))
                .collect(Collectors.toList());

        // Cache result
        positionCache.put(pos, result);
        return result;
    }

    public Double getZoneTemperatureAt(BlockPos pos) {
        List<TemperatureZone> zonesAtPos = getZonesAt(pos);
        if (zonesAtPos.isEmpty()) {
            return null;
        }

        // Handle based on zone type
        TemperatureZone primaryZone = zonesAtPos.get(0); // Highest priority
        double result = primaryZone.getTemperatureAt(pos);

        // For additive zones, sum all effects
        if (primaryZone.getType() == TemperatureZone.ZoneType.ADDITIVE) {
            for (int i = 1; i < zonesAtPos.size(); i++) {
                TemperatureZone zone = zonesAtPos.get(i);
                if (zone.getType() == TemperatureZone.ZoneType.ADDITIVE) {
                    result += zone.getTemperatureAt(pos);
                }
            }
        }

        return result;
    }

    public List<TemperatureZone> findZonesByName(String nameQuery) {
        String query = nameQuery.toLowerCase();
        return zones.values().stream()
                .filter(zone -> zone.getName().toLowerCase().contains(query))
                .collect(Collectors.toList());
    }

    public List<TemperatureZone> getZonesInArea(Box area) {
        return zones.values().stream()
                .filter(zone -> zone.getBounds().intersects(area))
                .collect(Collectors.toList());
    }

    private void clearPositionCache() {
        positionCache.clear();
    }

    public void tick() {
        cacheClearCounter++;
        if (cacheClearCounter >= CACHE_CLEAR_INTERVAL) {
            if (positionCache.size() > 1000) { // Only clear if cache is large
                clearPositionCache();
            }
            cacheClearCounter = 0;
        }
    }

    // ===== Statistics =====

    public int getZoneCount() {
        return zones.size();
    }

    public int getEnabledZoneCount() {
        return (int) zones.values().stream().filter(TemperatureZone::isEnabled).count();
    }

    public long getTotalZoneVolume() {
        return zones.values().stream().mapToLong(TemperatureZone::getVolume).sum();
    }

    // ===== Persistence =====

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        NbtList zonesList = new NbtList();
        for (TemperatureZone zone : zones.values()) {
            zonesList.add(zone.toNbt());
        }
        nbt.put("Zones", zonesList);
        nbt.putInt("Count", zones.size());
        return nbt;
    }

    public static TemperatureZoneManager fromNbt(ServerWorld world, NbtCompound nbt) {
        TemperatureZoneManager manager = new TemperatureZoneManager(world);

        NbtList zonesList = nbt.getList("Zones", 10); // 10 = compound type
        for (int i = 0; i < zonesList.size(); i++) {
            try {
                TemperatureZone zone = TemperatureZone.fromNbt(zonesList.getCompound(i));
                manager.zones.put(zone.getId(), zone);
            } catch (Exception e) {
                Temperaturem.LOGGER.error("Failed to load temperature zone {}", i, e);
            }
        }

        Temperaturem.LOGGER.info("Loaded {} temperature zones for world {}",
                manager.zones.size(), world.getRegistryKey().getValue());
        return manager;
    }
}
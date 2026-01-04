package net.IneiTsuki.temperaturem.seasons;

import net.IneiTsuki.temperaturem.Temperaturem;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.PersistentState;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SeasonManager extends PersistentState {

    private static final String DATA_NAME = "temperaturem_seasons";
    private static final Map<ServerWorld, SeasonManager> INSTANCES = new ConcurrentHashMap<>();

    // ===== Configuration =====
    private boolean enabled;
    private int seasonLengthDays;
    private boolean notifySeasonChange;

    // ===== State =====
    private Season currentSeason = Season.SPRING;
    private long seasonStartTick;

    private final ServerWorld world;

    // ===== Constructor =====
    private SeasonManager(ServerWorld world) {
        this.world = world;
        this.seasonStartTick = world.getTime();
        applyConfig();
    }

    // ===== Static access =====
    public static SeasonManager get(ServerWorld world) {
        return INSTANCES.computeIfAbsent(world, w ->
                w.getPersistentStateManager().getOrCreate(
                        nbt -> fromNbt(w, nbt),
                        () -> new SeasonManager(w),
                        DATA_NAME
                )
        );
    }

    public static void clearInstances() {
        INSTANCES.clear();
    }

    // ===== Configuration =====
    private void applyConfig() {
        SeasonConfig config = Temperaturem.getSeasonConfig();
        if (config == null) return;

        this.enabled = config.enabled;
        this.seasonLengthDays = Math.max(1, config.seasonLengthDays);
        this.notifySeasonChange = config.notifySeasonChange;
    }

    // ===== Tick =====
    public void tick() {
        if (!enabled) return;

        long worldTime = world.getTime();
        long ticksPerSeason = getTicksPerSeason();
        long elapsed = worldTime - seasonStartTick;

        // Handle multiple season skips
        while (elapsed >= ticksPerSeason) {
            advanceSeason();
            elapsed -= ticksPerSeason;
            seasonStartTick = worldTime - elapsed;
        }
    }

    private void advanceSeason() {
        Season old = currentSeason;
        currentSeason = currentSeason.next();
        markDirty();

        Temperaturem.LOGGER.info(
                "Season changed from {} to {} in world {}",
                old.getDisplayName(),
                currentSeason.getDisplayName(),
                world.getRegistryKey().getValue()
        );

        if (notifySeasonChange) {
            notifyPlayers(old, currentSeason);
        }
    }

    private void notifyPlayers(Season oldSeason, Season newSeason) {
        Text msg = Text.literal("Season changed: ")
                .formatted(Formatting.GOLD)
                .append(oldSeason.toText())
                .append(Text.literal(" → ").formatted(Formatting.GRAY))
                .append(newSeason.toText());

        world.getPlayers().forEach(p -> p.sendMessage(msg, false));
    }

    // ===== Temperature helpers =====
    private double getConfiguredModifier(Season season) {
        SeasonConfig c = Temperaturem.getSeasonConfig();
        return switch (season) {
            case SPRING -> c.springTempModifier;
            case SUMMER -> c.summerTempModifier;
            case AUTUMN -> c.autumnTempModifier;
            case WINTER -> c.winterTempModifier;
        };
    }

    private double getConfiguredMultiplier(Season season) {
        SeasonConfig c = Temperaturem.getSeasonConfig();
        return switch (season) {
            case SPRING -> c.springTempMultiplier;
            case SUMMER -> c.summerTempMultiplier;
            case AUTUMN -> c.autumnTempMultiplier;
            case WINTER -> c.winterTempMultiplier;
        };
    }

    public double getSeasonalTemperatureModifier() {
        if (!enabled) return 0.0;

        double progress = getSeasonProgress();
        Season next = currentSeason.next();

        if (progress > 0.75) {
            double t = (progress - 0.75) / 0.25;
            t = t * t * (3 - 2 * t);
            return getConfiguredModifier(currentSeason) +
                    (getConfiguredModifier(next) - getConfiguredModifier(currentSeason)) * t;
        }

        return getConfiguredModifier(currentSeason);
    }

    public double getSeasonalTemperatureMultiplier() {
        if (!enabled) return 1.0;

        double progress = getSeasonProgress();
        Season next = currentSeason.next();

        if (progress > 0.75) {
            double t = (progress - 0.75) / 0.25;
            t = t * t * (3 - 2 * t);
            return getConfiguredMultiplier(currentSeason) +
                    (getConfiguredMultiplier(next) - getConfiguredMultiplier(currentSeason)) * t;
        }

        return getConfiguredMultiplier(currentSeason);
    }

    // ===== Season info =====
    public double getSeasonProgress() {
        if (!enabled) return 0.0;
        long elapsed = world.getTime() - seasonStartTick;
        return Math.min(1.0, (double) elapsed / getTicksPerSeason());
    }

    public int getRemainingDays() {
        long remaining = getTicksPerSeason() - (world.getTime() - seasonStartTick);
        return Math.max(0, (int) Math.ceil(remaining / 24000.0));
    }

    public String getSeasonInfo() {
        return String.format("%s (Day %d/%d) - Progress: %.1f%%",
                currentSeason.getDisplayName(),
                seasonLengthDays - getRemainingDays(),
                seasonLengthDays,
                getSeasonProgress() * 100);
    }

    private long getTicksPerSeason() {
        return seasonLengthDays * 24000L;
    }

    // ===== Getters & Setters for API =====
    public Season getCurrentSeason() {
        return currentSeason;
    }

    public void setCurrentSeason(Season season) {
        if (season == null) return;
        Season old = this.currentSeason;
        this.currentSeason = season;
        this.seasonStartTick = world.getTime();
        markDirty();

        if (notifySeasonChange) {
            notifyPlayers(old, season);
        }

        Temperaturem.LOGGER.info(
                "Season manually set to {} in world {}",
                season.getDisplayName(),
                world.getRegistryKey().getValue()
        );
    }

    public void skipToNextSeason() {
        advanceSeason();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        markDirty();
    }

    public int getSeasonLengthDays() {
        return seasonLengthDays;
    }

    public void setSeasonLengthDays(int days) {
        if (days < 1) days = 1;
        if (days > 1000) days = 1000;
        this.seasonLengthDays = days;
        markDirty();
    }

    public boolean isNotifySeasonChange() {
        return notifySeasonChange;
    }

    public void setNotifySeasonChange(boolean notify) {
        this.notifySeasonChange = notify;
        markDirty();
    }

    // ===== Persistence =====
    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        nbt.putInt("CurrentSeason", currentSeason.getId());
        nbt.putLong("SeasonStartTick", seasonStartTick);
        return nbt;
    }

    public static SeasonManager fromNbt(ServerWorld world, NbtCompound nbt) {
        SeasonManager manager = new SeasonManager(world);
        manager.currentSeason = Season.fromId(nbt.getInt("CurrentSeason"));
        manager.seasonStartTick = nbt.getLong("SeasonStartTick");
        manager.applyConfig();
        return manager;
    }
}

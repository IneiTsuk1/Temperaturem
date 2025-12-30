package net.IneiTsuki.temperaturem.player.mixin;

import net.IneiTsuki.temperaturem.player.PlayerTemperatureManager;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerEntity.class)
public class ServerPlayerEntityMixin {

    @Unique
    private static final String NBT_KEY = "TemperatureMod:Temperature";

    // Inject into writeCustomDataToNbt to save temperature
    @Inject(method = "writeCustomDataToNbt", at = @At("TAIL"))
    private void saveTemperature(NbtCompound nbt, CallbackInfo ci) {
        ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;
        var temp = PlayerTemperatureManager.get(player);
        if (temp != null) {
            nbt.putInt(NBT_KEY, temp.get());
        }
    }

    // Inject into readCustomDataFromNbt to load temperature
    @Inject(method = "readCustomDataFromNbt", at = @At("TAIL"))
    private void loadTemperature(NbtCompound nbt, CallbackInfo ci) {
        ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;
        var temp = PlayerTemperatureManager.get(player);
        if (temp != null && nbt.contains(NBT_KEY)) {
            temp.set(nbt.getInt(NBT_KEY));
        }
    }
}
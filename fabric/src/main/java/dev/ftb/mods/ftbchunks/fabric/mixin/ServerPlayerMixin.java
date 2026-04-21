package dev.ftb.mods.ftbchunks.fabric.mixin;

import dev.ftb.mods.ftbchunks.fabric.FabricEventListeners;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public class ServerPlayerMixin {
    @Inject(method = "triggerDimensionChangeTriggers", at = @At("HEAD"))
    private void changeDimension(ServerLevel serverLevel, CallbackInfo ci) {
        ServerPlayer serverPlayer = (ServerPlayer) (Object) this;
        FabricEventListeners.get().playerChangedDimension(serverPlayer, serverLevel.dimension(), serverPlayer.level().dimension());
    }
}

package dev.ftb.mods.ftbchunks.fabric.mixin;

import dev.ftb.mods.ftbchunks.fabric.FabricEventListeners;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FarmlandBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FarmlandBlock.class)
public abstract class FarmlandBlockMixin {
    @Inject(method = "turnToDirt", at = @At("HEAD"), cancellable = true)
    private static void onTurnToDirt(Entity sourceEntity, BlockState state, Level level, BlockPos pos, CallbackInfo ci) {
        if (!FabricEventListeners.get().canFarmlandTrample(sourceEntity, pos)) {
            ci.cancel();
        }
    }
}

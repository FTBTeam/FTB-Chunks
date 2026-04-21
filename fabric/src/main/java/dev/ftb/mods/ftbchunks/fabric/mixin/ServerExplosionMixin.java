package dev.ftb.mods.ftbchunks.fabric.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import dev.ftb.mods.ftbchunks.fabric.FabricEventListeners;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ServerExplosion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Set;

@Mixin(ServerExplosion.class)
public class ServerExplosionMixin {
    @Inject(method = "calculateExplodedPositions", at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/objects/ObjectArrayList;<init>(Ljava/util/Collection;)V"))
    public void onCalculateExplodedPositions(CallbackInfoReturnable<List<BlockPos>> cir, @Local(name = "toBlowSet") Set<BlockPos> toBlowSet) {
        FabricEventListeners.get().handleServerExplosion((ServerExplosion) (Object) this, toBlowSet);
    }
}

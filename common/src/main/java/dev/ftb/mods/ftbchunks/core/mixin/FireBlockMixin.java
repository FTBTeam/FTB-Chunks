package dev.ftb.mods.ftbchunks.core.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.ftb.mods.ftbchunks.util.FireSpreadHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.FireBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(FireBlock.class)
public abstract class FireBlockMixin {
    @Shadow
    protected abstract int getIgniteOdds(LevelReader level, BlockPos pos);

    // Setting argsOnly = true will cause the mixin application to fail on Fabric, but not on NeoForge.  Go figure.
    @SuppressWarnings("LocalMayBeArgsOnly")
    @WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/FireBlock;getIgniteOdds(Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/core/BlockPos;)I"))
    private int ftbc$tick(FireBlock instance, LevelReader level, BlockPos pos, Operation<Integer> original, @Local(name = "pos") BlockPos origPos) {
        if (FireSpreadHelper.shouldPreventFireSpread(level, origPos, pos)) {
            return 0;
        }
        return getIgniteOdds(level, pos);
    }
}

package dev.ftb.mods.ftbchunks.core.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.ftb.mods.ftbchunks.util.FireSpreadHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(FireBlock.class)
public abstract class FireBlockMixin {
    @WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/FireBlock;getIgniteOdds(Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/core/BlockPos;)I"))
    private int ftbc$wrapGetIgniteOdds(FireBlock instance, LevelReader levelReader, BlockPos testPos, Operation<Integer> original, BlockState state, final ServerLevel level, final BlockPos pos, final RandomSource random) {
        if (FireSpreadHelper.shouldPreventFireSpread(level, pos, testPos)) {
            return 0;
        }
        return original.call(instance, level, testPos);
    }
}

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
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(FireBlock.class)
public abstract class FireBlockMixin {
    @WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/FireBlock;getIgniteOdds(Lnet/minecraft/world/level/LevelReader;Lnet/minecraft/core/BlockPos;)I"))
    private int ftbc$wrapGetIgniteOdds(FireBlock instance, LevelReader levelReader, BlockPos blockPos, Operation<Integer> original, @Local(name = "arg3", argsOnly = true) BlockPos pos) {
        if (FireSpreadHelper.shouldPreventFireSpread(levelReader, pos, blockPos)) {
            return 0;
        }
        return original.call(instance, levelReader, blockPos);
    }
}

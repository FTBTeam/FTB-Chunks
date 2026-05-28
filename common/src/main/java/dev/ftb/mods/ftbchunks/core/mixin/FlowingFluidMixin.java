package dev.ftb.mods.ftbchunks.core.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import dev.ftb.mods.ftbchunks.FlowingFluidHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

@Mixin(FlowingFluid.class)
public abstract class FlowingFluidMixin {
    @Inject(method = "getSpread", at = @At("RETURN"))
    public void onGetSpread(Level level, BlockPos blockPos, BlockState blockState, CallbackInfoReturnable<Map<Direction, FluidState>> cir, @Local(name = "map") Map<Direction,FluidState> map) {
        FlowingFluidHelper.applyFluidSpreadRestrictions(level, blockPos, map);
    }
}

package dev.ftb.mods.ftbchunks.core.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import dev.ftb.mods.ftbchunks.util.FlowingFluidHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
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
    public void onGetSpread(ServerLevel level, BlockPos pos, BlockState state, CallbackInfoReturnable<Map<Direction, FluidState>> cir, @Local(name = "result") Map<Direction,FluidState> result) {
        FlowingFluidHelper.applyFluidSpreadRestrictions(level, pos, result);
    }
}

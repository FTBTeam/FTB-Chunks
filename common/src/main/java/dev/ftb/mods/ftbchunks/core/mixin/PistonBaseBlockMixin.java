package dev.ftb.mods.ftbchunks.core.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import dev.architectury.platform.Platform;
import dev.ftb.mods.ftbchunks.api.FTBChunksProperties;
import dev.ftb.mods.ftbchunks.util.PistonHelper;
import dev.ftb.mods.ftbteams.api.property.PrivacyProperty;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.piston.PistonStructureResolver;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PistonBaseBlock.class)
public class PistonBaseBlockMixin {
    @Inject(method = "moveBlocks", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/piston/PistonStructureResolver;getToPush()Ljava/util/List;"), cancellable = true)
    public void onMoveBlocks(Level level, BlockPos blockPos, Direction direction, boolean extending, CallbackInfoReturnable<Boolean> cir, @Local PistonStructureResolver pistonStructureResolver) {
        if (PistonHelper.shouldPreventPistonMovement(level, blockPos, pistonStructureResolver)) {
            cir.setReturnValue(false);
        }
    }
}

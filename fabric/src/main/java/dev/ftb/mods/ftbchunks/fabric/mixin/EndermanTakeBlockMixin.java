package dev.ftb.mods.ftbchunks.fabric.mixin;

import dev.ftb.mods.ftbchunks.data.ClaimedChunk;
import dev.ftb.mods.ftbchunks.data.FTBChunksAPI;
import dev.ftb.mods.ftblibrary.math.ChunkDimPos;
import net.minecraft.world.entity.monster.EnderMan;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.world.entity.monster.EnderMan$EndermanTakeBlockGoal")
public class EndermanTakeBlockMixin {
    @Shadow @Final private EnderMan enderman;

    @Inject(method = "canUse", at = @At("HEAD"), cancellable = true)
    private void injected(CallbackInfoReturnable<Boolean> cir) {
        ClaimedChunk cc = FTBChunksAPI.getManager().getChunk(new ChunkDimPos(this.enderman));

        if (cc != null && !cc.allowMobGriefing()) {
            cir.setReturnValue(false);
        }
    }
}

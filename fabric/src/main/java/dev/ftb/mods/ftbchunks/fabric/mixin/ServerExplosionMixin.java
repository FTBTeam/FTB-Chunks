package dev.ftb.mods.ftbchunks.fabric.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import dev.ftb.mods.ftbchunks.FTBChunks;
import dev.ftb.mods.ftbchunks.api.ClaimedChunkManager;
import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import dev.ftb.mods.ftbchunks.fabric.FabricEventListeners;
import dev.ftb.mods.ftblibrary.math.ChunkDimPos;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ServerExplosion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Set;

@Mixin(ServerExplosion.class)
public class ServerExplosionMixin {
    @Inject(method = "calculateExplodedPositions", at = @At(value = "INVOKE", target = "Lit/unimi/dsi/fastutil/objects/ObjectArrayList;<init>(Ljava/util/Collection;)V"))
    public void onCalculateExplodedPositions(CallbackInfoReturnable<List<BlockPos>> cir, @Local(name = "toBlowSet") Set<BlockPos> toBlowSet) {
        FabricEventListeners.get().handleServerExplosion((ServerExplosion) (Object) this, toBlowSet);
    }

    @Inject(method = "hurtEntities", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;ignoreExplosion(Lnet/minecraft/world/level/Explosion;)Z"), cancellable = true)
    public void onHurtEntities(CallbackInfo ci, @Local(name = "entity") Entity entity) {
        if (FTBChunks.isNonLivingOrArmorStand(entity)) {
            ClaimedChunkManager manager = FTBChunksAPI.api().getManager();
            var cc = manager.getChunk(new ChunkDimPos(entity.level(), entity.blockPosition()));
            if (cc != null && !cc.getTeamData().canExplosionsDamageTerrain()) {
                ci.cancel();
            }
        }
    }
}

package dev.ftb.mods.ftbchunks.core.mixin;

import dev.ftb.mods.ftbchunks.FTBChunks;
import dev.ftb.mods.ftbchunks.api.ClaimedChunkManager;
import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import dev.ftb.mods.ftbchunks.api.FTBChunksProperties;
import dev.ftb.mods.ftblibrary.math.ChunkDimPos;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityMixin {
    @Shadow
    private BlockPos blockPosition;

    @Shadow
    private Level level;

    @Inject(method = "hurtOrSimulate", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;hurtServer(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)Z"), cancellable = true)
    public void onHurtOrSimulate(DamageSource source, float damage, CallbackInfoReturnable<Boolean> cir) {
        if (!FTBChunks.isNonLivingOrArmorStand((Entity) (Object) this)) {
            return;
        }
        ClaimedChunkManager manager = FTBChunksAPI.api().getManager();
        var cc = manager.getChunk(new ChunkDimPos(level, blockPosition));
        if (cc != null) {
            if (source.is(DamageTypeTags.IS_EXPLOSION) && !cc.getTeamData().canExplosionsDamageTerrain()) {
                cir.setReturnValue(false);
            } else if (source.getEntity() instanceof ServerPlayer serverPlayer
                    && !manager.getBypassProtection(serverPlayer.getUUID())
                    && !cc.getTeamData().canPlayerUse(serverPlayer, FTBChunksProperties.NONLIVING_ENTITY_ATTACK_MODE))
            {
                cir.setReturnValue(false);
            }
        }
    }
}

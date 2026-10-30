package dev.ftb.mods.ftbchunks.core.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.ftb.mods.ftbchunks.data.ClaimedChunkManagerImpl;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ChunkMap.class)
public abstract class ChunkMapMixin {
	@Shadow
	@Final
	ServerLevel level;

	@ModifyReturnValue(method = "anyPlayerCloseEnoughForSpawning", at = @At("RETURN"))
	private boolean ftbc$anyPlayerCloseEnoughForSpawning(boolean original, ChunkPos chunkPos) {
        if (original) {
            return true;
        }

        ClaimedChunkManagerImpl mgr = ClaimedChunkManagerImpl.getInstance();
        // it's possible for the claim manager to be null at this point, depending on what other mixins are in play...
        // https://github.com/FTBTeam/FTB-Mods-Issues/issues/1020
        return mgr != null && mgr.getForceLoadedChunks(level.dimension()).containsKey(chunkPos.toLong());
    }
}

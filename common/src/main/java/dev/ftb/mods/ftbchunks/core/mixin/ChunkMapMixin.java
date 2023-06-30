package dev.ftb.mods.ftbchunks.core.mixin;

import dev.ftb.mods.ftbchunks.data.FTBChunksAPI;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChunkMap.class)
public abstract class ChunkMapMixin {
	@Shadow
	@Final
	ServerLevel level;

	@Inject(method = "anyPlayerCloseEnoughForSpawning", at = @At("RETURN"), cancellable = true)
	private void anyPlayerCloseEnoughForSpawningFTBC(ChunkPos pos, CallbackInfoReturnable<Boolean> ci) {
		if (!ci.getReturnValue() && FTBChunksAPI.isChunkForceLoaded(level.dimension(), pos.x, pos.z)) {
			ci.setReturnValue(true);
		}
	}
}

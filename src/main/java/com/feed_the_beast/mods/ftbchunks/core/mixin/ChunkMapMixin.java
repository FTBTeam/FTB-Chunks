package com.feed_the_beast.mods.ftbchunks.core.mixin;

import com.feed_the_beast.mods.ftbchunks.FTBChunksConfig;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * @author LatvianModder
 */
@Mixin(ChunkMap.class)
public class ChunkMapMixin {
	@Shadow
	@Final
	private ServerLevel level;

	@Inject(method = "noPlayersCloseForSpawning", at = @At("RETURN"), cancellable = true)
	private void noPlayersCloseForSpawningFTBC(ChunkPos pos, CallbackInfoReturnable<Boolean> ci) {
		if (ci.getReturnValue() && FTBChunksConfig.patchChunkLoading(level, pos)) {
			ci.setReturnValue(false);
		}
	}
}

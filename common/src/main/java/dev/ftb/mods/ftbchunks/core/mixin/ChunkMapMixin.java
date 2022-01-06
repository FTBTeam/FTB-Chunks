package dev.ftb.mods.ftbchunks.core.mixin;

import dev.ftb.mods.ftbchunks.FTBChunksWorldConfig;
import dev.ftb.mods.ftbchunks.core.ChunkMapFTBC;
import dev.ftb.mods.ftbchunks.data.FTBChunksAPI;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * @author LatvianModder
 */
@Mixin(ChunkMap.class)
public abstract class ChunkMapMixin implements ChunkMapFTBC {
	@Shadow
	@Final
	ServerLevel level;

	@Inject(method = "anyPlayerCloseEnoughForSpawning", at = @At("RETURN"), cancellable = true)
	private void anyPlayerCloseEnoughForSpawningFTBC(ChunkPos pos, CallbackInfoReturnable<Boolean> ci) {
		if (ci.getReturnValue() && FTBChunksAPI.isManagerLoaded() && FTBChunksWorldConfig.patchChunkLoading(level, pos)) {
			ci.setReturnValue(true);
		}
	}

	@Override
	@Accessor("visibleChunkMap")
	public abstract Long2ObjectLinkedOpenHashMap<ChunkHolder> getChunksFTBC();
}

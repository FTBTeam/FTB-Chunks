package com.feed_the_beast.mods.ftbchunks.core.mixin;

import com.feed_the_beast.mods.ftbchunks.FTBChunksConfig;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.server.ChunkManager;
import net.minecraft.world.server.ServerWorld;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * @author LatvianModder
 */
@Mixin(ChunkManager.class)
public class ChunkManagerMixin
{
	@Shadow
	@Final
	private ServerWorld world;

	@Inject(method = "isOutsideSpawningRadius", at = @At("RETURN"), cancellable = true)
	public void isOutsideSpawningRadiusPatch(ChunkPos pos, CallbackInfoReturnable<Boolean> ci)
	{
		if (ci.getReturnValue() && FTBChunksConfig.patchChunkLoading(world, pos))
		{
			ci.setReturnValue(false);
		}
	}
}

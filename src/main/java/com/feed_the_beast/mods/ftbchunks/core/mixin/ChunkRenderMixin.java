package com.feed_the_beast.mods.ftbchunks.core.mixin;

import com.feed_the_beast.mods.ftbchunks.client.FTBChunksClient;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.util.math.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author LatvianModder
 */
@Mixin(ChunkRenderDispatcher.ChunkRender.class)
public class ChunkRenderMixin
{
	@Inject(method = "setNotDirty", at = @At("RETURN"))
	private void clearNeedsUpdateFTBC(CallbackInfo ci)
	{
		FTBChunksClient.rerenderCache.add(new ChunkPos(((ChunkRenderDispatcher.ChunkRender) (Object) this).getOrigin()));
	}
}

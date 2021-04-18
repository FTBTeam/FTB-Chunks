package dev.ftb.mods.ftbchunks.core.mixin;

import dev.ftb.mods.ftbchunks.client.FTBChunksClient;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author LatvianModder
 */
@Mixin(ChunkRenderDispatcher.RenderChunk.class)
public class ChunkRenderMixin {
	@Inject(method = "setNotDirty", at = @At("RETURN"))
	private void clearNeedsUpdateFTBC(CallbackInfo ci) {
		FTBChunksClient.rerender(((ChunkRenderDispatcher.RenderChunk) (Object) this).getOrigin());
	}
}

package dev.ftb.mods.ftbchunks.core.mixin;

import dev.ftb.mods.ftbchunks.FTBChunks;
import net.minecraft.Util;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.TimeUnit;

/**
 * @author LatvianModder
 */
@Mixin(Util.class)
public abstract class UtilMixin {
	@Inject(method = "shutdownExecutors", at = @At("RETURN"))
	private static void shutdownExecutorsFTBC(CallbackInfo ci) {
		FTBChunks.LOGGER.info("Shutting down map thread");
		FTBChunks.EXECUTOR.shutdown();

		boolean b;
		try {
			b = FTBChunks.EXECUTOR.awaitTermination(3L, TimeUnit.SECONDS);
		} catch (InterruptedException var3) {
			b = false;
		}

		if (!b) {
			FTBChunks.EXECUTOR.shutdownNow();
		}
	}
}

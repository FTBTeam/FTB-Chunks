package com.feed_the_beast.mods.ftbchunks.core.mixin;

import com.feed_the_beast.mods.ftbchunks.FTBChunks;
import net.minecraft.util.Util;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.TimeUnit;

/**
 * @author LatvianModder
 */
@Mixin(Util.class)
public abstract class UtilMixin
{
	@Inject(method = "shutdown", at = @At("RETURN"))
	private static void shutdownExecutorsFTBC(CallbackInfo ci)
	{
		FTBChunks.EXECUTOR_SERVICE.shutdown();

		boolean bl2;

		try
		{
			bl2 = FTBChunks.EXECUTOR_SERVICE.awaitTermination(3L, TimeUnit.SECONDS);
		}
		catch (InterruptedException var3)
		{
			bl2 = false;
		}

		if (!bl2)
		{
			FTBChunks.EXECUTOR_SERVICE.shutdownNow();
		}
	}
}
package dev.ftb.mods.ftbchunks.client.forge;

import dev.ftb.mods.ftbchunks.FTBChunks;
import dev.ftb.mods.ftbchunks.client.FTBChunksClient;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.MinecraftForge;

public class FTBChunksClientImpl {
	public static void registerPlatform() {
		MinecraftForge.EVENT_BUS.addListener(FTBChunksClientImpl::renderWorldLastForge);
	}

	private static void renderWorldLastForge(RenderWorldLastEvent event) {
		((FTBChunksClient) FTBChunks.PROXY).renderWorldLast(event.getMatrixStack());
	}
}

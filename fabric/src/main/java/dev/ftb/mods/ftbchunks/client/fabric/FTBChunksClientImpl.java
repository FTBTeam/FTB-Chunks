package dev.ftb.mods.ftbchunks.client.fabric;

import dev.ftb.mods.ftbchunks.FTBChunks;
import dev.ftb.mods.ftbchunks.client.FTBChunksClient;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.server.level.ServerLevel;

public class FTBChunksClientImpl {
	public static void registerPlatform() {
		WorldRenderEvents.AFTER_TRANSLUCENT.register(FTBChunksClientImpl::renderWorldLastFabric);
	}

	private static void renderWorldLastFabric(WorldRenderContext context) {
		((FTBChunksClient) FTBChunks.PROXY).renderWorldLast(context.matrixStack(), context.projectionMatrix(), context.camera(), context.tickDelta());
	}
}

package dev.ftb.mods.ftbchunks.fabric;

import dev.architectury.platform.Platform;
import dev.ftb.mods.ftbchunks.client.FTBChunksClient;
import dev.ftb.mods.ftbchunks.client.fabric.FTBChunksClientImpl;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;

public class FTBChunksFabricClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		WorldRenderEvents.AFTER_ENTITIES.register(this::renderWorldLastFabric);

		if (Platform.isModLoaded("canvas")) {
			CanvasIntegration.init();
		}
	}

	private void renderWorldLastFabric(WorldRenderContext context) {
//		FTBChunksClient.INSTANCE.renderWorldLast(context.matrices(), context.positionMatrix(), context.camera(), context.tickCounter());
	}
}

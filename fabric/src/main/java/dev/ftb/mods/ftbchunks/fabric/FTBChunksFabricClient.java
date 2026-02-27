package dev.ftb.mods.ftbchunks.fabric;

import dev.architectury.platform.Platform;
import dev.ftb.mods.ftbchunks.client.FTBChunksClient;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldExtractionContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;

public class FTBChunksFabricClient implements ClientModInitializer {
	private FTBChunksClient client;

	@Override
	public void onInitializeClient() {
		client = FTBChunksClient.INSTANCE.init();

		WorldRenderEvents.END_EXTRACTION.register(this::renderLevelStage);

		if (Platform.isModLoaded("canvas")) {
			CanvasIntegration.init();
		}
	}

	private void renderLevelStage(WorldExtractionContext context) {
		client.getInWorldIconRenderer().renderLevelStage(context.viewMatrix(), context.camera().position(), context.tickCounter());
	}
}

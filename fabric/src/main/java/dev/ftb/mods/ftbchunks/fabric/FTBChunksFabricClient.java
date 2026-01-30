package dev.ftb.mods.ftbchunks.fabric;

import dev.architectury.platform.Platform;
import dev.ftb.mods.ftbchunks.client.FTBChunksClient;
import dev.ftb.mods.ftbchunks.client.minimap.MinimapPIPRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.SpecialGuiElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldExtractionContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;

public class FTBChunksFabricClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		WorldRenderEvents.END_EXTRACTION.register(this::renderLevelStage);

		if (Platform.isModLoaded("canvas")) {
			CanvasIntegration.init();
		}

		SpecialGuiElementRegistry.register(ctx -> new MinimapPIPRenderer(ctx.vertexConsumers()));
	}

	private void renderLevelStage(WorldExtractionContext context) {
		FTBChunksClient.INSTANCE.renderLevelStage(context.viewMatrix(), context.camera().position(), context.tickCounter());
	}
}

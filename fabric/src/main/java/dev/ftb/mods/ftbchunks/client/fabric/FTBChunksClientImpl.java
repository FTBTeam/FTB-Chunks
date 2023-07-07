package dev.ftb.mods.ftbchunks.client.fabric;

import dev.ftb.mods.ftbchunks.client.FTBChunksClient;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.KeyMapping;

public class FTBChunksClientImpl {
	public static void registerPlatform() {
		WorldRenderEvents.AFTER_TRANSLUCENT.register(FTBChunksClientImpl::renderWorldLastFabric);
	}

	private static void renderWorldLastFabric(WorldRenderContext context) {
		FTBChunksClient.INSTANCE.renderWorldLast(context.matrixStack(), context.projectionMatrix(), context.camera(), context.tickDelta());
	}

	public static boolean doesKeybindMatch(KeyMapping keyMapping, int keyCode, int scanCode, int modifiers) {
		// TODO how can we handle key modifiers on Fabric?
		return keyMapping.matches(keyCode, scanCode);
	}
}

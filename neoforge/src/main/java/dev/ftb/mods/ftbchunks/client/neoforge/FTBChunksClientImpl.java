package dev.ftb.mods.ftbchunks.client.neoforge;

import dev.ftb.mods.ftbchunks.client.FTBChunksClient;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.lwjgl.glfw.GLFW;

public class FTBChunksClientImpl {
	public static void registerPlatform() {
		NeoForge.EVENT_BUS.register(FTBChunksClientImpl.class);
	}

	@SubscribeEvent
	public static void renderLevelStageForge(RenderLevelStageEvent event) {
		if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
			FTBChunksClient.INSTANCE.renderWorldLast(event.getPoseStack(), event.getProjectionMatrix(), Minecraft.getInstance().getEntityRenderDispatcher().camera, event.getPartialTick());
		}
	}

	public static boolean doesKeybindMatch(KeyMapping keyMapping, int keyCode, int scanCode, int modifiers) {
		if (keyMapping.matches(keyCode, scanCode)) {
			return switch (keyMapping.getKeyModifier()) {
				case NONE -> modifiers == 0;
				case SHIFT ->  (modifiers & GLFW.GLFW_MOD_SHIFT) != 0;
				case CONTROL ->  (modifiers & GLFW.GLFW_MOD_CONTROL) != 0;
				case ALT ->  (modifiers & GLFW.GLFW_MOD_ALT) != 0;
			};
		}
		return false;
	}
}

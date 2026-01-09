package dev.ftb.mods.ftbchunks.client.neoforge;

import dev.ftb.mods.ftbchunks.client.FTBChunksClient;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.lwjgl.glfw.GLFW;

public class FTBChunksClientImpl {
	public static void registerPlatform() {
		NeoForge.EVENT_BUS.register(FTBChunksClientImpl.class);
	}

	@SubscribeEvent
	public static void renderLevelStageForge(RenderLevelStageEvent.AfterParticles event) {
        FTBChunksClient.INSTANCE.renderWorldLast(event.getPoseStack(), event.getModelViewMatrix(),
                event.getModelViewMatrix(), event.getLevelRenderState().cameraRenderState, Minecraft.getInstance().getDeltaTracker());
	}

	public static boolean doesKeybindMatch(KeyMapping keyMapping, KeyEvent event) {
		if (keyMapping.matches(event)) {
			return switch (keyMapping.getKeyModifier()) {
				case NONE -> event.modifiers() == 0;
				case SHIFT -> (event.modifiers() & GLFW.GLFW_MOD_SHIFT) != 0;
				case CONTROL -> (event.modifiers() & GLFW.GLFW_MOD_CONTROL) != 0;
				case ALT ->  (event.modifiers() & GLFW.GLFW_MOD_ALT) != 0;
			};
		}
		return false;
	}
}

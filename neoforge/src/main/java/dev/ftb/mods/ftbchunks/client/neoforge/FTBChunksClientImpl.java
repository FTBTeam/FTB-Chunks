package dev.ftb.mods.ftbchunks.client.neoforge;

import dev.ftb.mods.ftbchunks.client.FTBChunksClient;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.jspecify.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

public class FTBChunksClientImpl {
	// arch expectplatform
	@SuppressWarnings("unused")
    public static void registerPlatform() {
	}

	// arch expectplatform
	@SuppressWarnings("unused")
    public static boolean doesKeybindMatch(@Nullable KeyMapping keyMapping, KeyEvent event) {
		if (keyMapping != null && keyMapping.matches(event)) {
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

package dev.ftb.mods.ftbchunks.client.neoforge;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.input.KeyEvent;
import org.jspecify.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

// arch ExpectPlatform
@SuppressWarnings("unused")
public class FTBChunksClientImpl {
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

package dev.ftb.mods.ftbchunks.client.fabric;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.input.KeyEvent;

public class FTBChunksClientImpl {
	// arch expectplatform
	@SuppressWarnings("unused")
	public static void registerPlatform() {
	}

	// arch expectplatform
	@SuppressWarnings("unused")
	public static boolean doesKeybindMatch(KeyMapping keyMapping, KeyEvent keyEvent) {
		// TODO how can we handle key modifiers on Fabric?
		return keyMapping.matches(keyEvent);
	}
}

package dev.ftb.mods.ftbchunks.client.fabric;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.input.KeyEvent;

// arch expectplatform
@SuppressWarnings("unused")
public class FTBChunksClientImpl {
	public static boolean doesKeybindMatch(KeyMapping keyMapping, KeyEvent keyEvent) {
		// TODO how can we handle key modifiers on Fabric?
		return keyMapping.matches(keyEvent);
	}
}

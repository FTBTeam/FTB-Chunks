package dev.ftb.mods.ftbchunks.client.fabric;

import com.mojang.blaze3d.platform.InputConstants;
import dev.ftb.mods.ftbchunks.client.FTBChunksClient;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public class FTBChunksClientImpl {
	public static void registerPlatform() {
		FTBChunksClient.openMapKey = new KeyMapping("key.ftbchunks.map", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_M, "key.categories.ui");
		KeyBindingHelper.registerKeyBinding(FTBChunksClient.openMapKey);
	}
}

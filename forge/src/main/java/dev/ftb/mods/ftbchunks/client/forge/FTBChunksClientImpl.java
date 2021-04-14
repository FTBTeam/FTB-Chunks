package dev.ftb.mods.ftbchunks.client.forge;

import com.mojang.blaze3d.platform.InputConstants;
import dev.ftb.mods.ftbchunks.FTBChunks;
import dev.ftb.mods.ftbchunks.client.FTBChunksClient;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import org.lwjgl.glfw.GLFW;

public class FTBChunksClientImpl {
	public static void registerPlatform() {
		FTBChunksClient.openMapKey = new KeyMapping("key.ftbchunks.map", KeyConflictContext.IN_GAME, KeyModifier.NONE, InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_M, "key.categories.ui");
		ClientRegistry.registerKeyBinding(FTBChunksClient.openMapKey);
		MinecraftForge.EVENT_BUS.addListener(FTBChunksClientImpl::renderLast);
	}

	private static void renderLast(RenderWorldLastEvent event) {
		((FTBChunksClient) FTBChunks.PROXY).renderWorldLast(event.getMatrixStack());
	}
}

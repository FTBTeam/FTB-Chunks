package dev.ftb.mods.ftbchunks.client.forge;

import dev.ftb.mods.ftbchunks.FTBChunks;
import dev.ftb.mods.ftbchunks.client.FTBChunksClient;
import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.client.event.RenderLevelLastEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class FTBChunksClientImpl {
	public static void registerPlatform() {
		MinecraftForge.EVENT_BUS.register(FTBChunksClientImpl.class);
	}

	@SubscribeEvent
	public static void renderLevelLastForge(RenderLevelLastEvent event) {
		((FTBChunksClient) FTBChunks.PROXY).renderWorldLast(event.getPoseStack(), event.getProjectionMatrix(), Minecraft.getInstance().getEntityRenderDispatcher().camera, event.getPartialTick());
	}
}

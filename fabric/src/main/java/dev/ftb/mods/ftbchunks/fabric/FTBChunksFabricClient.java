package dev.ftb.mods.ftbchunks.fabric;

import dev.architectury.platform.Platform;
import net.fabricmc.api.ClientModInitializer;

public class FTBChunksFabricClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		if (Platform.isModLoaded("canvas")) {
			CanvasIntegration.init();
		}
	}
}

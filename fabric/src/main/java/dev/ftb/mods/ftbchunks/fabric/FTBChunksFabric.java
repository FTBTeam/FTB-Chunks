package dev.ftb.mods.ftbchunks.fabric;

import dev.architectury.platform.Platform;
import dev.ftb.mods.ftbchunks.FTBChunks;
import dev.ftb.mods.ftbchunks.compat.waystones.WaystonesCompat;
import net.fabricmc.api.ModInitializer;
import net.minecraft.server.level.ServerLevel;

public class FTBChunksFabric implements ModInitializer {
	@Override
	public void onInitialize() {
		FTBChunks.instance = new FTBChunks();

		if (Platform.isModLoaded("waystones")) {
			WaystonesCompat.init();
		}
	}
}

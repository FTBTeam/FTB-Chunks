package dev.ftb.mods.ftbchunks.fabric;

import dev.ftb.mods.ftbchunks.FTBChunks;
import net.fabricmc.api.ModInitializer;

public class FTBChunksFabric implements ModInitializer {
	@Override
	public void onInitialize() {
		FTBChunks.instance = new FTBChunks();
	}
}

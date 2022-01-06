package dev.ftb.mods.ftbchunks.forge;

import dev.architectury.platform.Platform;
import dev.architectury.platform.forge.EventBuses;
import dev.ftb.mods.ftbchunks.FTBChunks;
import dev.ftb.mods.ftbchunks.compat.waystones.WaystonesCompat;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(FTBChunks.MOD_ID)
public class FTBChunksForge {
	public FTBChunksForge() {
		EventBuses.registerModEventBus(FTBChunks.MOD_ID, FMLJavaModLoadingContext.get().getModEventBus());
		FTBChunks.instance = new FTBChunks();

		if (Platform.isModLoaded("waystones")) {
			initWaystonesCompat();
		}
	}

	private void initWaystonesCompat() {
		WaystonesCompat.init();
	}
}

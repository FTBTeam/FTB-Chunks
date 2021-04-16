package dev.ftb.mods.ftbchunks.forge;

import dev.ftb.mods.ftbchunks.FTBChunks;
import me.shedaniel.architectury.platform.forge.EventBuses;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(FTBChunks.MOD_ID)
public class FTBChunksForge {
	public FTBChunksForge() {
		EventBuses.registerModEventBus(FTBChunks.MOD_ID, FMLJavaModLoadingContext.get().getModEventBus());
		FTBChunks.instance = new FTBChunks();
	}
}

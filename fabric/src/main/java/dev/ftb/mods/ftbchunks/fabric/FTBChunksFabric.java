package dev.ftb.mods.ftbchunks.fabric;

import dev.architectury.platform.Platform;
import dev.ftb.mods.ftbchunks.FTBChunks;
import dev.ftb.mods.ftbchunks.compat.waystones.WaystonesCompat;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.server.level.ServerPlayer;

public class FTBChunksFabric implements ModInitializer {
	@Override
	public void onInitialize() {
		FTBChunks.instance = new FTBChunks();

		PlayerBlockBreakEvents.BEFORE.register((level, player, blockPos, blockState, blockEntity) -> {
			if (player instanceof ServerPlayer serverPlayer) {
				return !FTBChunks.blockBreak(level, blockPos, blockState, serverPlayer, null).isFalse();
			}
			 return true;
		});

		if (Platform.isModLoaded("waystones")) {
			WaystonesCompat.init();
		}
	}
}

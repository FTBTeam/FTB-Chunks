package dev.ftb.mods.ftbchunks.fabric;

import dev.architectury.platform.Platform;
import dev.ftb.mods.ftbchunks.FTBChunks;
import dev.ftb.mods.ftbchunks.compat.waystones.WaystonesCompat;
import dev.ftb.mods.ftbchunks.data.FTBChunksAPI;
import dev.ftb.mods.ftbchunks.data.Protection;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;

public class FTBChunksFabric implements ModInitializer {
	@Override
	public void onInitialize() {
		FTBChunks.instance = new FTBChunks();

		// TODO remove when arch PR merged
		AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
			if (player instanceof ServerPlayer && !(entity instanceof LivingEntity) && FTBChunksAPI.getManager().protect(player, hand, entity.blockPosition(), Protection.ATTACK_NONLIVING_ENTITY, entity)) {
				return InteractionResult.FAIL;
			}
			return InteractionResult.PASS;
		});

		if (Platform.isModLoaded("waystones")) {
			WaystonesCompat.init();
		}
	}
}

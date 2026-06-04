package dev.ftb.mods.ftbchunks;

import dev.architectury.injectables.annotations.ExpectPlatform;
import dev.ftb.mods.ftbchunks.api.ClaimedChunkManager;
import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import dev.ftb.mods.ftbchunks.api.FTBChunksProperties;
import dev.ftb.mods.ftblibrary.math.ChunkDimPos;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.Block;

public class FTBCUtils {
	@ExpectPlatform
	public static boolean isRail(Block block) {
		throw new AssertionError();
	}


	/**
	 * Used after various events have been cancelled server-side; client may already have updated the held item for the
	 * player, but it needs to be brought back in sync with the server.
	 *
	 * @param sp   the player
	 * @param hand the hand being used
	 */
	public static void forceHeldItemSync(ServerPlayer sp, InteractionHand hand) {
		if (sp.connection != null) {
			switch (hand) {
				case MAIN_HAND -> sp.connection.send(new ClientboundContainerSetSlotPacket(-2, 0, sp.getInventory().selected, sp.getItemInHand(hand)));
				case OFF_HAND -> sp.connection.send(new ClientboundContainerSetSlotPacket(-2, 0, Inventory.SLOT_OFFHAND, sp.getItemInHand(hand)));
			}
		}
	}

	public static boolean shouldProtectNonLivingEntity(Entity e, DamageSource source) {
		if (!FTBChunks.isNonLivingOrArmorStand(e) || e.level().isClientSide()) {
			return false;
		}

		ClaimedChunkManager manager = FTBChunksAPI.api().getManager();
		var cc = manager.getChunk(new ChunkDimPos(e.level(), e.blockPosition()));
		if (cc != null) {
			if (source.is(DamageTypeTags.IS_EXPLOSION) && !cc.getTeamData().canExplosionsDamageTerrain()) {
				return true;
			}
			return source.getEntity() instanceof ServerPlayer serverPlayer
					&& !manager.getBypassProtection(serverPlayer.getUUID())
					&& !cc.getTeamData().canPlayerUse(serverPlayer, FTBChunksProperties.NONLIVING_ENTITY_ATTACK_MODE);
		}

		return false;
	}
}
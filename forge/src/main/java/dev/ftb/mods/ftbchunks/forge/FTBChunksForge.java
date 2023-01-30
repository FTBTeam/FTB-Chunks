package dev.ftb.mods.ftbchunks.forge;

import com.google.common.collect.Sets;
import dev.architectury.platform.Platform;
import dev.architectury.platform.forge.EventBuses;
import dev.ftb.mods.ftbchunks.FTBChunks;
import dev.ftb.mods.ftbchunks.compat.waystones.WaystonesCompat;
import dev.ftb.mods.ftbchunks.data.ClaimedChunk;
import dev.ftb.mods.ftbchunks.data.FTBChunksAPI;
import dev.ftb.mods.ftbchunks.data.FTBChunksTeamData;
import dev.ftb.mods.ftbchunks.data.Protection;
import dev.ftb.mods.ftblibrary.math.ChunkDimPos;
import dev.ftb.mods.ftbteams.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.data.Team;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.world.ForgeChunkManager;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Mod(FTBChunks.MOD_ID)
public class FTBChunksForge {
	public FTBChunksForge() {
		EventBuses.registerModEventBus(FTBChunks.MOD_ID, FMLJavaModLoadingContext.get().getModEventBus());
		MinecraftForge.EVENT_BUS.addListener(this::entityInteractSpecific);
		MinecraftForge.EVENT_BUS.addListener(this::attackNonLivingEntity);
		FTBChunks.instance = new FTBChunks();

		ForgeChunkManager.setForcedChunkLoadingCallback(FTBChunks.MOD_ID, this::validateLoadedChunks);

		if (Platform.isModLoaded("waystones")) {
			WaystonesCompat.init();
		}
	}

	// TODO remove when arch PR merged
	private void attackNonLivingEntity(AttackEntityEvent event) {
		if (event.getPlayer() instanceof ServerPlayer sp && !(event.getTarget() instanceof LivingEntity) && FTBChunksAPI.getManager().protect(sp, event.getPlayer().getUsedItemHand(), sp.blockPosition(), Protection.ATTACK_NONLIVING_ENTITY, event.getTarget())) {
			event.setCanceled(true);
		}
	}

	/**
	 * Temporary hack until Architectury hopefully adds direct support for this. Needed to prevent interaction
	 * with Armor Stands (and probably anything where the specific interaction position is important). NOTE:
	 * currently broken in 1.18.2 due to a Forge bug:
	 * <a href="https://github.com/MinecraftForge/MinecraftForge/issues/8143">...</a>
	 *
	 * @param event the event
	 */
	private void entityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
		if (!event.getPlayer().level.isClientSide && FTBChunksAPI.getManager().protect(event.getPlayer(), event.getHand(), event.getEntity().blockPosition(), Protection.INTERACT_ENTITY, event.getTarget())) {
			event.setCancellationResult(InteractionResult.FAIL);
			event.setCanceled(true);
		}
	}

	private void validateLoadedChunks(ServerLevel level, ForgeChunkManager.TicketHelper ticketHelper) {
		// Clean up any ticking Forge chunk-loading tickets for chunks which aren't both claimed and force-loaded
		ticketHelper.getEntityTickets().forEach((id, chunks) -> {
			Set<Long> toRemove = new HashSet<>();
			chunks.getSecond().forEach(l -> {
				ClaimedChunk cc = FTBChunksAPI.getManager().getChunk(new ChunkDimPos(level.dimension(), new ChunkPos(l)));
				if (cc == null || !cc.teamData.getTeamId().equals(id) || !cc.isForceLoaded()) {
					toRemove.add(l);
				}
			});
			toRemove.forEach(l -> ticketHelper.removeTicket(id, l, true));
			if (!toRemove.isEmpty()) {
				FTBChunks.LOGGER.info("cleaned up {} stale Forge chunkloading tickets for team ID {} in dimension {}",
						toRemove.size(), id, level.dimension().location());
			}
		});
	}
}

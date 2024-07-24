package dev.ftb.mods.ftbchunks.forge;

import dev.architectury.platform.forge.EventBuses;
import dev.ftb.mods.ftbchunks.FTBChunks;
import dev.ftb.mods.ftbchunks.api.FTBChunksTags;
import dev.ftb.mods.ftbchunks.api.Protection;
import dev.ftb.mods.ftbchunks.data.ClaimedChunkImpl;
import dev.ftb.mods.ftbchunks.data.ClaimedChunkManagerImpl;
import dev.ftb.mods.ftblibrary.math.ChunkDimPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.world.ForgeChunkManager;
import net.minecraftforge.event.entity.EntityMobGriefingEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import java.util.HashSet;
import java.util.Set;

@Mod(FTBChunks.MOD_ID)
public class FTBChunksForge {
	public FTBChunksForge() {
		EventBuses.registerModEventBus(FTBChunks.MOD_ID, FMLJavaModLoadingContext.get().getModEventBus());
		MinecraftForge.EVENT_BUS.addListener(this::entityInteractSpecific);
		MinecraftForge.EVENT_BUS.addListener(this::mobGriefing);
		FTBChunks.instance = new FTBChunks();

		ForgeChunkManager.setForcedChunkLoadingCallback(FTBChunks.MOD_ID, this::validateLoadedChunks);
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
		if (!event.getEntity().level().isClientSide && ClaimedChunkManagerImpl.getInstance().shouldPreventInteraction(event.getEntity(), event.getHand(), event.getEntity().blockPosition(), Protection.INTERACT_ENTITY, event.getTarget())) {
			event.setCancellationResult(InteractionResult.FAIL);
			event.setCanceled(true);
		}
	}

	private void mobGriefing(EntityMobGriefingEvent event) {
		// we could do this for all mob griefing but that's arguably OP (could trivialize wither fights, for example)
		// expose this as a tag so pack makers can change to what they want (default tag with only enderman)
		// due to current limitations on fabric this tag will only work on NeoForge, while fabric only supports enderman
		// Note: explicit check for server-side needed due to Optifine brain-damage
		if (event.getEntity().getType().is(FTBChunksTags.Entities.ENTITY_MOB_GRIEFING_BLACKLIST_TAG) && !event.getEntity().level().isClientSide()) {
			ClaimedChunkImpl cc = ClaimedChunkManagerImpl.getInstance().getChunk(new ChunkDimPos(event.getEntity()));

			if (cc != null && !cc.allowMobGriefing()) {
				event.setResult(Event.Result.DENY);
			}
		}
	}

	private void validateLoadedChunks(ServerLevel level, ForgeChunkManager.TicketHelper ticketHelper) {
		FTBChunks.LOGGER.debug("validating chunk tickets for level {}", level.dimension().location());

		ticketHelper.getEntityTickets().forEach((id, chunks) -> {
			FTBChunks.LOGGER.debug("validating {} ticking chunk tickets for {}", chunks.getSecond().size(), id);

			// non-ticking tickets - shouldn't have any of these; purge just in case (older releases of Chunks registered them)
			Set<Long> toRemoveNon = new HashSet<>(chunks.getFirst());
			if (!toRemoveNon.isEmpty()) {
				toRemoveNon.forEach(l -> ticketHelper.removeTicket(id, l, false));
				FTBChunks.LOGGER.info("purged {} non-ticking Forge chunkloading tickets for team ID {} in dimension {}",
						toRemoveNon.size(), id, level.dimension().location());
			}

			// ticking tickets - purge if the chunk is either unclaimed or should not be offline-force-loaded
			Set<Long> toRemove = new HashSet<>();
			chunks.getSecond().forEach(l -> {
				ClaimedChunkImpl cc = ClaimedChunkManagerImpl.getInstance().getChunk(new ChunkDimPos(level.dimension(), new ChunkPos(l)));
				if (cc == null || !cc.getTeamData().getTeamId().equals(id) || !cc.isActuallyForceLoaded()) {
					toRemove.add(l);
				}
			});
			if (!toRemove.isEmpty()) {
				toRemove.forEach(l -> ticketHelper.removeTicket(id, l, true));
				FTBChunks.LOGGER.info("cleaned up {} stale ticking Forge chunkloading tickets for team ID {} in dimension {}",
						toRemove.size(), id, level.dimension().location());
			}
		});
	}
}

package dev.ftb.mods.ftbchunks.forge;

import dev.architectury.platform.Platform;
import dev.architectury.platform.forge.EventBuses;
import dev.ftb.mods.ftbchunks.FTBChunks;
import dev.ftb.mods.ftbchunks.compat.waystones.WaystonesCompat;
import dev.ftb.mods.ftbchunks.data.FTBChunksAPI;
import dev.ftb.mods.ftbchunks.data.Protection;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.world.ForgeChunkManager;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(FTBChunks.MOD_ID)
public class FTBChunksForge {
	public FTBChunksForge() {
		EventBuses.registerModEventBus(FTBChunks.MOD_ID, FMLJavaModLoadingContext.get().getModEventBus());
		MinecraftForge.EVENT_BUS.addListener(this::entityInteractSpecific);
		FTBChunks.instance = new FTBChunks();

		ForgeChunkManager.setForcedChunkLoadingCallback(FTBChunks.MOD_ID, this::validateLoadedChunks);

		if (Platform.isModLoaded("waystones")) {
			WaystonesCompat.init();
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
		if (!event.getEntity().level.isClientSide && FTBChunksAPI.getManager().protect(event.getEntity(), event.getHand(), event.getEntity().blockPosition(), Protection.INTERACT_ENTITY, event.getTarget())) {
			event.setCancellationResult(InteractionResult.FAIL);
			event.setCanceled(true);
		}
	}

	private void validateLoadedChunks(ServerLevel level, ForgeChunkManager.TicketHelper ticketHelper) {
		// It's safe to just remove all stored Forge tickets, since we do our own forced chunk tracking,
		//   and we'll just re-force all the chunks we care about shortly after this is called (which
		//   also re-registers them with Forge).
		// This should prevent our forced list getting out of sync with Forge's for whatever reason.
		ticketHelper.getEntityTickets().forEach((id, chunks) -> {
			FTBChunks.LOGGER.info(String.format("cleaning up %d non-ticking and %d ticking tickets for %s", chunks.getFirst().size(), chunks.getSecond().size(), id));
			ticketHelper.removeAllTickets(id);
		});
	}
}

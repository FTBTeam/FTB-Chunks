package dev.ftb.mods.ftbchunks.neoforge;

import dev.ftb.mods.ftbchunks.FTBChunks;
import dev.ftb.mods.ftbchunks.api.Protection;
import dev.ftb.mods.ftbchunks.data.ClaimedChunkImpl;
import dev.ftb.mods.ftbchunks.data.ClaimedChunkManagerImpl;
import dev.ftb.mods.ftblibrary.math.ChunkDimPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.monster.EnderMan;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityMobGriefingEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@Mod(FTBChunks.MOD_ID)
public class FTBChunksForge {
	public FTBChunksForge(IEventBus modEventBus) {
		NeoForge.EVENT_BUS.addListener(this::entityInteractSpecific);
		NeoForge.EVENT_BUS.addListener(this::mobGriefing);

		ForceLoading.setup(modEventBus);

		FTBChunks.instance = new FTBChunks();
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
		// enderman block stealing is the most common annoyance, and this also has parity with the fabric support
		// Note: explicit check for server-side needed due to Optifine brain-damage
		if (event.getEntity() instanceof EnderMan && !event.getEntity().level().isClientSide()) {
			ClaimedChunkImpl cc = ClaimedChunkManagerImpl.getInstance().getChunk(new ChunkDimPos(event.getEntity()));

			if (cc != null && !cc.allowMobGriefing()) {
				event.setResult(Event.Result.DENY);
			}
		}
	}
}

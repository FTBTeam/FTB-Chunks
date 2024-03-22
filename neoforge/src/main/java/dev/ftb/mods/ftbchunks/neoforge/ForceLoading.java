package dev.ftb.mods.ftbchunks.neoforge;

import dev.ftb.mods.ftbchunks.FTBChunks;
import dev.ftb.mods.ftbchunks.data.ClaimedChunkImpl;
import dev.ftb.mods.ftbchunks.data.ClaimedChunkManagerImpl;
import dev.ftb.mods.ftblibrary.math.ChunkDimPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.world.chunk.RegisterTicketControllersEvent;
import net.neoforged.neoforge.common.world.chunk.TicketController;
import net.neoforged.neoforge.common.world.chunk.TicketHelper;

import java.util.HashSet;
import java.util.Set;

public class ForceLoading {
    static TicketController ticketController;

    static void setup(IEventBus modEventBus) {
        modEventBus.addListener(ForceLoading::registerTicketController);
    }

    private static void registerTicketController(RegisterTicketControllersEvent event) {
        ticketController = new TicketController(new ResourceLocation(FTBChunks.MOD_ID, "default"), ForceLoading::validateLoadedChunks);

        event.register(ticketController);
    }

    private static void validateLoadedChunks(ServerLevel level, TicketHelper ticketHelper) {
        FTBChunks.LOGGER.debug("validating chunk tickets for level {}", level.dimension().location());

        ticketHelper.getEntityTickets().forEach((id, chunks) -> {
            FTBChunks.LOGGER.debug("validating {} ticking chunk tickets for {}", chunks.ticking().size(), id);

            // non-ticking tickets - shouldn't have any of these; purge just in case (older releases of Chunks registered them)
            Set<Long> toRemoveNon = new HashSet<>(chunks.nonTicking());
            if (!toRemoveNon.isEmpty()) {
                toRemoveNon.forEach(l -> ticketHelper.removeTicket(id, l, false));
                FTBChunks.LOGGER.info("purged {} non-ticking Forge chunkloading tickets for team ID {} in dimension {}",
                        toRemoveNon.size(), id, level.dimension().location());
            }

            // ticking tickets - purge if the chunk is either unclaimed or should not be offline-force-loaded
            Set<Long> toRemove = new HashSet<>();
            chunks.ticking().forEach(l -> {
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
